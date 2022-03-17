/*
 *     Injector is a runtime class modification library for Kotlin
 *     Copyright (C) 2021  Conor Byrne
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.cbyrne.injector.transform

import codes.som.anthony.koffee.assembleBlock
import codes.som.anthony.koffee.assembleClass
import codes.som.anthony.koffee.insns.jvm.*
import codes.som.anthony.koffee.modifiers.public
import codes.som.anthony.koffee.modifiers.static
import dev.cbyrne.injector.position.InjectPosition
import dev.cbyrne.injector.provider.InjectorParams
import dev.cbyrne.injector.provider.MethodInjector
import dev.cbyrne.injector.provider.ReturnInfo
import dev.cbyrne.injector.util.addMethod
import dev.cbyrne.injector.util.readBytes
import fr.stardustenterprises.deface.engine.api.IClassTransformer
import net.gudenau.lib.unsafe.Unsafe
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.VarInsnNode
import java.lang.reflect.Modifier
import java.nio.file.Files
import java.security.ProtectionDomain

object InjectorClassTransformer : IClassTransformer {
    private const val INJECTOR_NAMESPACE = "injector$"
    private const val debug: Boolean = true

    private val debugDumpDir by lazy { Files.createTempDirectory("injector_dump") }

    private val exclusions: MutableList<String> =
        mutableListOf(
            "java.",
            "kotlin.",
            "sun.",
            "javax.",
            "argo.",
            "org.objectweb.asm.",
            "dev.cbyrne.injector.",
            "codes.som.anthony.koffee.",
        )

    private var hookContainerIndex = 0

    private val cache = mutableMapOf<String, ByteArray>()
    private val hookContainerCache = mutableMapOf<MethodInjector<*>, ClassNode>()

    val methodInjectors = mutableListOf<MethodInjector<*>>()

    /**
     * Adds any Injector references in methods when required
     *
     * @param redefinedClass The existing [Class] (if being redefined)
     * @param classLoader The [ClassLoader] being utilized
     * @param className The name of the class, this should be in the format of path/to/class and not path.to.class
     * @param protectionDomain The class's [ProtectionDomain]
     * @param classBuffer The classfile buffer
     *
     * @return the transformed class bytes if there are any method injectors, otherwise [classBuffer]
     */
    override fun transformClass(
        redefinedClass: Class<*>?,
        classLoader: ClassLoader?,
        className: String,
        protectionDomain: ProtectionDomain?,
        classBuffer: ByteArray,
    ): ByteArray? {
        // Don't transform not-defined classes that may
        // be used in this class.
        if (redefinedClass == null) {
            val isExcluded =
                exclusions.any {
                    className.startsWith(it.replace('.', '/'))
                }
            if (isExcluded) {
                return null
            }
        }
        // Don't transform hooks...
        if (className.startsWith("$INJECTOR_NAMESPACE/")) {
            return null
        }

        // Store class buffer in cache for later use
        cache.computeIfAbsent(className) { classBuffer }

        // Load the class bytes into a class node
        val classReader = ClassReader(cache[className])
        val classNode = ClassNode()
        classReader.accept(classNode, ClassReader.EXPAND_FRAMES)

        // If the class didn't get modified, return null
        if (!transformClassNode(classNode, classLoader)) {
            return null
        }

        // Write the transformed class and return it
        val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        classNode.accept(classWriter)
        val bytecode = classWriter.toByteArray()

        // Write the file to local disk for debugging purposes
        if (debug) {
            val file = debugDumpDir.resolve("$className.class")
            file.parent.toFile().mkdirs()

            Files.write(file, bytecode)
        }

        // Cache the current class bytecode for reuse later
        cache[className] = bytecode

        return bytecode
    }

    private fun defineClass(
        classNode: ClassNode,
        cwFlags: Int = ClassWriter.COMPUTE_FRAMES,
        classLoader: ClassLoader? = null,
    ): Class<*> {
        val className = classNode.name

        val classWriter = ClassWriter(cwFlags)
        classNode.accept(classWriter)
        val bytecode = classWriter.toByteArray()

        if (debug) {
            val file = debugDumpDir.resolve("$className.class")
            file.parent.toFile().mkdirs()

            Files.write(file, bytecode)
        }

        return Unsafe.defineClass<Any>(
            className,
            bytecode,
            0,
            bytecode.size,
            classLoader,
            null
        )
    }

    private fun transformClassNode(
        classNode: ClassNode,
        classLoader: ClassLoader?,
    ): Boolean {
        // Check if any injectors exist for this class
        val methodInjectors = methodInjectors.filter {
            it.className == classNode.name
        }
        if (methodInjectors.isEmpty()) return false

        var hookUsed = false
        val hookContainerClass = assembleClass(
            public,
            "$INJECTOR_NAMESPACE/${classNode.name}" +
                "_Hook_" +
                hookContainerIndex,
            Opcodes.V1_8
        ) {}
        hookContainerIndex++

        // Apply method injectors
        methodInjectors.forEach { injector ->
            // Get a method for our injector, if none exists,
            // go to the next entry
            val method = classNode.methods.firstOrNull {
                it.name == injector.method && it.desc == injector.descriptor
            } ?: return@forEach

            this.methodInjectors.remove(injector)
            hookUsed = true

            if (debug) {
                println(
                    "[InjectorClassTransformer] Applying injector for " +
                        "${classNode.name}.${method.name}${method.desc}" +
                        " @ ${injector.position}"
                )
            }

            // Read the Unit's class
            val codeNode = ClassNode()
            val codeReader = ClassReader(injector.code.javaClass.readBytes())
            codeReader.accept(codeNode, ClassReader.EXPAND_FRAMES)

            // Take the bytecode from Unit#invoke
            // and write it to our own function
            val invokeMethod = codeNode.methods.first {
                it.name == "invoke" &&
                    (it.access and Opcodes.ACC_SYNTHETIC == 0)
            }

            val injectorMethodName = "injector\$method" +
                methodInjectors.indexOf(injector)

            val insns = InsnList()
            invokeMethod.instructions.map {
                if (it is VarInsnNode) {
                    it.`var` -= 1
                }
                it
            }.forEach(insns::add)

            hookContainerClass.addMethod(
                public + static,
                injectorMethodName,
                invokeMethod.desc,
                insns
            )

            // Make an insnList to invoke our injector method
            val (insnList) = assembleBlock {
                // Create a new array list and store it
                // in the index of the last slot index + 1
                val paramsArraySlot = method.maxLocals + 1
                val fieldsMapSlot = paramsArraySlot + 1
                val returnInfoSlot = fieldsMapSlot + 1
                val injectorParamsSlot = returnInfoSlot + 1

                val isStatic = if (Modifier.isStatic(method.access)) 0 else 1

                // Create a new array list
                new(ArrayList::class)
                dup
                invokespecial(ArrayList::class, "<init>", void)
                astore(paramsArraySlot)

                // Create a new hashmap for the fields in this class
                new(HashMap::class)
                dup
                invokespecial(HashMap::class, "<init>", void)
                astore(fieldsMapSlot)

                // Add all parameters to the array
                // NOTE: This is done on each injectorMethod{x} call as
                // they can change during the method execution
                // The previous local type and offset
                var previousParameterType: Type? = null
                var previousParameterOffset = 0
                Type.getArgumentTypes(method.desc)
                    .forEachIndexed { index, type ->
                        // Load the array list
                        aload(paramsArraySlot)

                        // Add 1 to the slot if it's a long or a double,
                        // these take up 2 slots, so we should offset it by 1
                        val offset =
                            if (previousParameterType?.sort == Type.LONG ||
                                previousParameterType?.sort == Type.DOUBLE
                            ) 1
                            else 0

                        // Load the next parameter and convert
                        // it to a non-primitive if required
                        instructions.add(
                            primitiveConversionInsnList(
                                index +
                                    isStatic +
                                    offset +
                                    previousParameterOffset,
                                type
                            )
                        )

                        // Add the parameter to the list
                        invokevirtual(
                            java.util.ArrayList::class,
                            "add",
                            boolean,
                            java.lang.Object::class
                        )
                        pop

                        // Store the previous type and offset
                        previousParameterType = type
                        previousParameterOffset = offset
                    }

                classNode.fields.forEach { field ->
                    aload(fieldsMapSlot)
                    ldc(field.name)
                    if ((field.access and Opcodes.ACC_STATIC) != 0) {
                        getstatic(classNode.name, field)
                    } else {
                        aload(0)
                        getfield(classNode.name, field)
                    }
                    instructions.add(
                        primitiveConversionInsnList(Type.getType(field.desc))
                    )

                    invokeinterface(
                        java.util.Map::class,
                        "put",
                        java.lang.Object::class,
                        java.lang.Object::class,
                        java.lang.Object::class
                    )
                    pop
                }

                // val returnInfo = ReturnInfo()
                new(ReturnInfo::class.java)
                dup
                invokespecial(ReturnInfo::class.java, "<init>", void)
                astore(returnInfoSlot)

                // val params = InjectorParams(array, returnInfo, hashMap)
                new(InjectorParams::class)
                dup
                aload(paramsArraySlot)
                aload(fieldsMapSlot)
                aload(returnInfoSlot)
                invokespecial(
                    InjectorParams::class,
                    "<init>",
                    void,
                    List::class,
                    Map::class,
                    ReturnInfo::class
                )
                astore(injectorParamsSlot)

                // Call the injectorMethod{x} with the
                // current class instance and parameter list
                // injectorMethod0(this, injectorParams))
                aload_0
                aload(injectorParamsSlot)
                invokestatic(
                    hookContainerClass.name,
                    injectorMethodName,
                    invokeMethod.desc
                )

                // Go to the label retIfTrue if the method was cancelled
                aload(returnInfoSlot)
                invokevirtual(
                    ReturnInfo::class.java,
                    "getCancelled",
                    boolean
                )
                ifeq(L["retIfTrue"])

                // Simplified version of this in Kotlin:
                //
                // if (methodReturnType.sort == Type.VOID) return
                // else return returnInfo.getReturnValue()
                //
                val methodReturnType = Type.getReturnType(method.desc)
                if (methodReturnType.sort == Type.VOID) {
                    // If the method return type is void,
                    // then just insert a return instruction
                    _return
                } else {
                    // If the method returns a value,
                    // get the return value provided by the Injector,
                    // cast it then return it
                    aload(returnInfoSlot)
                    invokevirtual(
                        ReturnInfo::class.java,
                        "getReturnValue",
                        Any::class
                    )
                    instructions.add(returnInstructionForType(methodReturnType))
                }
                +L["retIfTrue"]
            }

            // Invoke our injector method at the specified position
            when (injector.position::class) {
                InjectPosition.BeforeAll::class -> {
                    method.instructions.insert(insnList)
                }

                InjectPosition.BeforeReturn::class -> {
                    method.instructions.insertBefore(
                        method.instructions.last.previous,
                        insnList
                    )
                }

                InjectPosition.Invoke::class -> {
                    val pos = injector.position as InjectPosition.Invoke
                    val targetInstruction = method.instructions
                        .filterIsInstance<MethodInsnNode>()
                        .first {
                            it.name == pos.name &&
                                it.desc == pos.descriptor &&
                                it.owner == pos.owner
                        }

                    when (pos.position) {
                        InjectPosition.InvokePosition.AFTER ->
                            method.instructions.insert(
                                targetInstruction,
                                insnList
                            )
                        InjectPosition.InvokePosition.BEFORE ->
                            method.instructions.insertBefore(
                                targetInstruction,
                                insnList
                            )
                    }
                }
            }
        }

        if (hookUsed) {
            defineClass(hookContainerClass, classLoader = classLoader)
        }

        return true
    }

    /**
     * Creates a return instruction for an Object type
     * This will handle casting if it's an object and
     * converting it to a primitive if required.
     *
     * The variable must already be on the stack
     *
     * @param type The return type of your method
     * @return an [InsnList] that returns the variable
     */
    private fun returnInstructionForType(type: Type): InsnList =
        assembleBlock {
            when (type.sort) {
                Type.INT -> {
                    checkcast(java.lang.Integer::class)
                    invokevirtual(
                        java.lang.Integer::class,
                        "intValue",
                        int
                    )
                    ireturn
                }
                Type.FLOAT -> {
                    checkcast(java.lang.Float::class)
                    invokevirtual(
                        java.lang.Float::class,
                        "floatValue",
                        float
                    )
                    freturn
                }
                Type.LONG -> {
                    checkcast(java.lang.Long::class)
                    invokevirtual(
                        java.lang.Long::class,
                        "longValue",
                        long
                    )
                    lreturn
                }
                Type.DOUBLE -> {
                    checkcast(java.lang.Double::class)
                    invokevirtual(
                        java.lang.Double::class,
                        "doubleValue",
                        double
                    )
                    dreturn
                }
                Type.BOOLEAN -> {
                    checkcast(java.lang.Boolean::class)
                    invokevirtual(
                        java.lang.Boolean::class,
                        "booleanValue",
                        boolean
                    )
                    ireturn
                }
                Type.SHORT -> {
                    checkcast(java.lang.Short::class)
                    invokevirtual(
                        java.lang.Short::class,
                        "shortValue",
                        short
                    )
                    ireturn
                }
                Type.BYTE -> {
                    checkcast(java.lang.Byte::class)
                    invokevirtual(
                        java.lang.Byte::class,
                        "byteValue",
                        byte
                    )
                    ireturn
                }
                Type.CHAR -> {
                    checkcast(java.lang.Character::class)
                    invokevirtual(
                        java.lang.Character::class,
                        "charValue",
                        char
                    )
                    ireturn
                }
                Type.OBJECT -> {
                    checkcast(type)
                    areturn
                }
                Type.ARRAY -> {
                    checkcast(type)
                    areturn
                }
            }
        }.first

    /**
     * Generates an [InsnList] to convert a primitive (int, float, long, etc.)
     * to a reference type (Integer, Float, Long, etc.)
     * If the passed index is already a reference type
     * (String, Object, Array, etc.), it will just add [aload] to the [InsnList]
     *
     * @param index The index of the primitive on the local variable table
     * @param type The type of the primitive
     */
    private fun primitiveConversionInsnList(index: Int, type: Type): InsnList =
        assembleBlock {
            when (type.sort) {
                Type.INT -> {
                    iload(index)
                    invokestatic(java.lang.Integer::class, "valueOf", java.lang.Integer::class, int)
                }
                Type.FLOAT -> {
                    fload(index)
                    invokestatic(java.lang.Float::class, "valueOf", java.lang.Float::class, float)
                }
                Type.LONG -> {
                    lload(index)
                    invokestatic(java.lang.Long::class, "valueOf", java.lang.Long::class, long)
                }
                Type.DOUBLE -> {
                    dload(index)
                    invokestatic(java.lang.Double::class, "valueOf", java.lang.Double::class, double)
                }
                Type.BOOLEAN -> {
                    iload(index)
                    invokestatic(java.lang.Boolean::class, "valueOf", java.lang.Boolean::class, boolean)
                }
                Type.SHORT -> {
                    iload(index)
                    invokestatic(java.lang.Short::class, "valueOf", java.lang.Short::class, short)
                }
                Type.BYTE -> {
                    iload(index)
                    invokestatic(java.lang.Byte::class, "valueOf", java.lang.Byte::class, byte)
                }
                Type.CHAR -> {
                    iload(index)
                    invokestatic(java.lang.Character::class, "valueOf", java.lang.Character::class, char)
                }
                Type.OBJECT -> aload(index)
                Type.ARRAY -> aload(index)
            }
        }.first

    private fun primitiveConversionInsnList(type: Type): InsnList =
        assembleBlock {
            when (type.sort) {
                Type.INT -> {
                    invokestatic(java.lang.Integer::class, "valueOf", java.lang.Integer::class, int)
                }
                Type.FLOAT -> {
                    invokestatic(java.lang.Float::class, "valueOf", java.lang.Float::class, float)
                }
                Type.LONG -> {
                    invokestatic(java.lang.Long::class, "valueOf", java.lang.Long::class, long)
                }
                Type.DOUBLE -> {
                    invokestatic(java.lang.Double::class, "valueOf", java.lang.Double::class, double)
                }
                Type.BOOLEAN -> {
                    invokestatic(java.lang.Boolean::class, "valueOf", java.lang.Boolean::class, boolean)
                }
                Type.SHORT -> {
                    invokestatic(java.lang.Short::class, "valueOf", java.lang.Short::class, short)
                }
                Type.BYTE -> {
                    invokestatic(java.lang.Byte::class, "valueOf", java.lang.Byte::class, byte)
                }
                Type.CHAR -> {
                    invokestatic(java.lang.Character::class, "valueOf", java.lang.Character::class, char)
                }
            }
        }.first
}
