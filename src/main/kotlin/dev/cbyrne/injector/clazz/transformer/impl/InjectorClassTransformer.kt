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

package dev.cbyrne.injector.clazz.transformer.impl

import codes.som.anthony.koffee.assembleBlock
import codes.som.anthony.koffee.insns.jvm.*
import codes.som.anthony.koffee.modifiers.public
import dev.cbyrne.injector.Injector
import dev.cbyrne.injector.clazz.transformer.IClassTransformer
import dev.cbyrne.injector.position.InjectPosition
import dev.cbyrne.injector.provider.InjectorParams
import dev.cbyrne.injector.provider.MethodInjector
import dev.cbyrne.injector.util.addMethod
import dev.cbyrne.injector.util.readBytes
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodInsnNode
import java.lang.reflect.Modifier

class InjectorClassTransformer(private val debug: Boolean = false) : IClassTransformer {
    /**
     * Adds any Injector references in methods when required
     *
     * @param name The name of the class, this should be in the format of path/to/class and not path.to.class
     * @param classBytes The bytes of the class
     *
     * @return the transformed class bytes if there are any method injectors, otherwise [classBytes]
     */
    override fun transformClass(name: String, classBytes: ByteArray): ByteArray {
        // Load the class bytes into a class node
        val classReader = ClassReader(classBytes)
        val classNode = ClassNode()
        classReader.accept(classNode, ClassReader.EXPAND_FRAMES)

        // Write the transformed class and return it
        val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        transformClassNode(classNode).accept(classWriter)

        return classWriter.toByteArray()
    }

    override fun transformClassNode(classNode: ClassNode): ClassNode {
        // Check if any injectors exist for this class
        val methodInjectors = Injector.methodInjectors.filter { it.className == classNode.name }
        if (methodInjectors.isEmpty()) return classNode

        // Apply method injectors
        methodInjectors.forEach { injector ->
            // Get a method for our injector, if none exists, go to the next entry
            val method = classNode.methods.firstOrNull { it.name == injector.method && it.desc == injector.descriptor }
                ?: return@forEach
            if (debug) println("[InjectorClassTransformer] Applying injector for ${classNode.name}#${method.name}")

            // Read the Unit's class
            val codeClassNode = ClassNode()
            val codeClassReader = ClassReader(injector.code.javaClass.readBytes())
            codeClassReader.accept(codeClassNode, ClassReader.EXPAND_FRAMES)

            // Take the bytecode from Unit#invoke and write it to our own function
            val invokeMethod =
                codeClassNode.methods.first { it.name == "invoke" && it.access and Opcodes.ACC_SYNTHETIC == 0 }
            val injectorMethodName = "injectorMethod${methodInjectors.indexOf(injector)}"
            classNode.addMethod(public, injectorMethodName, invokeMethod.desc, invokeMethod.instructions)

            // Make an insnList to invoke our injector method
            val (insnList) = assembleBlock {
                // The previous local type and offset
                var previousType: Type? = null
                var previousOffset = 0

                // Create a new array list and store it in the index of the last slot index + 1
                val arraySlot = method.maxLocals + 1
                val isStatic = if (Modifier.isStatic(method.access)) 0 else 1

                // Create a new array list
                new(ArrayList::class)
                dup
                invokespecial(ArrayList::class, "<init>", void)
                astore(arraySlot)

                // Add all parameters to the array
                // NOTE: This is done on each injectorMethod{x} call as they can change during the method execution
                Type.getArgumentTypes(method.desc).forEachIndexed { index, type ->
                    // Load the array list
                    aload(arraySlot)

                    // Add 1 to the slot if it's a long or a double, these take up 2 slots so we should offset it by 1
                    val offset = if (previousType?.sort == Type.LONG || previousType?.sort == Type.DOUBLE) 1 else 0

                    // Load the next parameter and convert it to a non-primitive if required
                    instructions.add(primitiveConversionInsnList(index + isStatic + offset + previousOffset, type))

                    // Add the parameter to the list
                    invokevirtual(java.util.ArrayList::class, "add", boolean, java.lang.Object::class)
                    pop

                    // Store the previous type and offset
                    previousType = type
                    previousOffset = offset
                }

                // val returnInfo = MethodInjector.returnInfo()
                new(MethodInjector.ReturnInfo::class)
                dup
                invokespecial(MethodInjector.ReturnInfo::class, "<init>", void)
                astore(arraySlot + 1)

                // val params = InjectorParams(array, returnInfo)
                new(InjectorParams::class)
                dup
                aload(arraySlot)
                aload(arraySlot + 1)
                invokespecial(InjectorParams::class, "<init>", void, List::class, MethodInjector.ReturnInfo::class)
                astore(arraySlot + 2)

                // Call the injectorMethod{x} with the current class instance and parameter list
                // injectorMethod0(this, returnInfo))
                aload_0
                dup
                aload(arraySlot + 2)
                invokevirtual(classNode.name, injectorMethodName, invokeMethod.desc)

                // Go to the label retIfTrue if the method was cancelled
                aload(arraySlot + 1)
                invokevirtual(MethodInjector.ReturnInfo::class, "getCancelled", boolean)
                ifeq(L["retIfTrue"])

                // Simplified version of this in Kotlin:
                //
                // if (methodReturnType.sort == Type.VOID) return
                // else return returnInfo.getReturnValue()
                //
                val methodReturnType = Type.getReturnType(method.desc)
                if (methodReturnType.sort == Type.VOID) {
                    // If the method return type is void, then just insert a return instruction
                    _return
                } else {
                    // If the method returns a value, get the return value provided by the Injector, cast it then return it
                    aload(arraySlot + 1)
                    invokevirtual(MethodInjector.ReturnInfo::class, "getReturnValue", Any::class)
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
                    method.instructions.insertBefore(method.instructions.last.previous, insnList)
                }

                InjectPosition.Invoke::class -> {
                    val pos = injector.position as InjectPosition.Invoke
                    val targetInstruction = method.instructions.filterIsInstance<MethodInsnNode>()
                        .first { it.name == pos.name && it.desc == pos.descriptor && it.owner == pos.owner }

                    when (pos.position) {
                        InjectPosition.InvokePosition.AFTER -> method.instructions.insert(targetInstruction, insnList)
                        InjectPosition.InvokePosition.BEFORE -> method.instructions.insertBefore(
                            targetInstruction,
                            insnList
                        )
                    }
                }
            }
        }

        return classNode
    }

    /**
     * Creates a return instruction for an Object type
     * This will handle casting if it's an object and converting it to a primitive if required
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
                    invokevirtual(java.lang.Integer::class, "intValue", int)
                    ireturn
                }
                Type.FLOAT -> {
                    checkcast(java.lang.Float::class)
                    invokevirtual(java.lang.Float::class, "floatValue", float)
                    freturn
                }
                Type.LONG -> {
                    checkcast(java.lang.Long::class)
                    invokevirtual(java.lang.Long::class, "longValue", long)
                    lreturn
                }
                Type.DOUBLE -> {
                    checkcast(java.lang.Double::class)
                    invokevirtual(java.lang.Double::class, "doubleValue", double)
                    dreturn
                }
                Type.BOOLEAN -> {
                    checkcast(java.lang.Boolean::class)
                    invokevirtual(java.lang.Boolean::class, "booleanValue", boolean)
                    ireturn
                }
                Type.SHORT -> {
                    checkcast(java.lang.Short::class)
                    invokevirtual(java.lang.Short::class, "shortValue", short)
                    ireturn
                }
                Type.BYTE -> {
                    checkcast(java.lang.Byte::class)
                    invokevirtual(java.lang.Byte::class, "byteValue", byte)
                    ireturn
                }
                Type.CHAR -> {
                    checkcast(java.lang.Character::class)
                    invokevirtual(java.lang.Character::class, "charValue", char)
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
     * Generates an [InsnList] to convert a primitive (int, float, long, etc.) to a reference type (Integer, Float, Long, etc.)
     * If the passed index is already a reference type (String, Object, Array, etc.), it will just add [aload] to the [InsnList]
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
}
