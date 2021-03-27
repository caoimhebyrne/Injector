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

package dev.dreamhopping.injector.clazz.transformer.impl

import codes.som.anthony.koffee.assembleBlock
import codes.som.anthony.koffee.insns.jvm.*
import codes.som.anthony.koffee.modifiers.public
import dev.dreamhopping.injector.Injector
import dev.dreamhopping.injector.clazz.transformer.IClassTransformer
import dev.dreamhopping.injector.position.InjectPosition
import dev.dreamhopping.injector.util.addMethod
import dev.dreamhopping.injector.util.readBytes
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import java.lang.reflect.Modifier
import java.util.*
import kotlin.collections.ArrayList

class InjectorClassTransformer : IClassTransformer {
    /**
     * Adds any Injector references in methods when required
     *
     * @param name The name of the class, this should be in the format of path/to/class and not path.to.class
     * @param classBytes The bytes of the class
     *
     * @return the transformed class bytes if there are any method injectors, otherwise [classBytes]
     */
    override fun transformClass(name: String, classBytes: ByteArray): ByteArray {
        // Check if any injectors exist for this class
        val methodInjectors = Injector.methodInjectors.filter { it.className == name }
        if (methodInjectors.isEmpty()) return classBytes

        // Load the class bytes into a class node
        val classReader = ClassReader(classBytes)
        val classNode = ClassNode()
        classReader.accept(classNode, ClassReader.EXPAND_FRAMES)

        // Apply method injectors
        methodInjectors.forEach { injector ->
            // Get a method for our injector, if none exists, go to the next entry
            val method = classNode.methods.firstOrNull { it.name == injector.method && it.desc == injector.descriptor }
                ?: return@forEach
            println("[InjectorClassTransformer] Applying injector for ${classNode.name}#${method.name} at position ${injector.position}")

            // Read the Unit's class
            val codeClassNode = ClassNode()
            val codeClassReader = ClassReader(injector.code.javaClass.readBytes())
            codeClassReader.accept(codeClassNode, ClassReader.EXPAND_FRAMES)

            // Take the bytecode from Unit#invoke and write it to our own function
            val invokeMethod =
                codeClassNode.methods.first { it.name == "invoke" && it.access and Opcodes.ACC_SYNTHETIC == 0 }
            val injectorMethodName = "injectorMethod${methodInjectors.indexOf(injector)}"
            classNode.addMethod(public, injectorMethodName, invokeMethod.desc, instructions = invokeMethod.instructions)

            // Make an insnList to invoke our injector method
            val (insnList) = assembleBlock {
                // Create a new array list and store it in the index of the last slot index + 1
                var previousType: Type? = null
                var previousOffset = 0
                val base = 0.takeIf { Modifier.isStatic(method.access) } ?: 1
                val arraySlot =
                    method.instructions.filterIsInstance<VarInsnNode>().maxByOrNull { it.`var` * 2 }?.`var` ?: base

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
                    instructions.add(primitiveConversionInsnList(index + base + offset + previousOffset, type))

                    // Add the parameter to the list
                    invokevirtual(java.util.ArrayList::class, "add", boolean, java.lang.Object::class)
                    pop

                    // Store the previous type and offset
                    previousType = type
                    previousOffset = offset
                }

                // Call the injectorMethod{x} with the current class instance and parameter list
                aload_0
                dup
                aload(arraySlot)
                invokevirtual(name, injectorMethodName, invokeMethod.desc)
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

        // Write the transformed class and return it
        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
        classNode.accept(classWriter)

        return classWriter.toByteArray()
    }

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
