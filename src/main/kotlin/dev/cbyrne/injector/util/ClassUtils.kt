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

package dev.cbyrne.injector.util

import codes.som.anthony.koffee.MethodAssembly
import codes.som.anthony.koffee.modifiers.Modifiers
import dev.cbyrne.injector.clazz.transformer.impl.InjectorClassTransformer
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodNode

fun Class<*>.readBytes(): ByteArray {
    val resourcePath = "${name.replace(".", "/")}.class"
    val classStream = InjectorClassTransformer::class.java.classLoader.getResourceAsStream(resourcePath)
        ?: error("Failed to read bytes")

    return classStream.use { it.readBytes() }
}

/**
 * Adapted from [https://github.com/videogame-hacker/Koffee]
 */
fun ClassNode.addMethod(
    access: Modifiers, name: String, desc: String, instructions: InsnList? = null,
    routine: (MethodAssembly.() -> Unit)? = null
) {
    val methodNode = MethodNode()
    methodNode.access = access.access
    methodNode.name = name
    methodNode.desc = desc
    methodNode.instructions = instructions
    methodNode.exceptions = listOf()

    val methodAssembly = MethodAssembly(methodNode)
    if (routine != null) routine(methodAssembly)

    this.methods.add(methodNode)
}