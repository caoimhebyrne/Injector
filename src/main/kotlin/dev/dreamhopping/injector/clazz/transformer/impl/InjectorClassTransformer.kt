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
import codes.som.anthony.koffee.insns.jvm.aload_0
import codes.som.anthony.koffee.insns.jvm.invokevirtual
import codes.som.anthony.koffee.modifiers.public
import codes.som.anthony.koffee.types.void
import dev.dreamhopping.injector.Injector
import dev.dreamhopping.injector.clazz.transformer.IClassTransformer
import dev.dreamhopping.injector.position.InjectPosition
import dev.dreamhopping.injector.util.addMethod
import dev.dreamhopping.injector.util.readBytes
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.*

class InjectorClassTransformer : IClassTransformer {
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

            // Take the bytecode from Unit#invoke()V and write it to our own function
            val invokeMethod = codeClassNode.methods.first { it.name == "invoke" && it.desc == "()V" }
            val injectorMethodName = "injectorMethod${methodInjectors.indexOf(injector)}"
            classNode.addMethod(public, injectorMethodName, void, instructions = invokeMethod.instructions)

            // Make an insnList to invoke our injector method
            val (insnList) = assembleBlock {
                aload_0
                invokevirtual(name, injectorMethodName, "()V")
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
}
