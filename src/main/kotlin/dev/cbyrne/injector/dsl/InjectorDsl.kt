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

package dev.cbyrne.injector.dsl

import codes.som.anthony.koffee.types.TypeLike
import codes.som.anthony.koffee.types.coerceType
import dev.cbyrne.injector.Injector
import dev.cbyrne.injector.position.InjectPosition
import dev.cbyrne.injector.provider.InjectorParams
import org.objectweb.asm.Type
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

val beforeAll = InjectPosition.BeforeAll
val beforeReturn = InjectPosition.BeforeTail

fun afterInvoke(className: String, methodName: String, descriptor: String) =
    InjectPosition.Invoke(className, methodName, descriptor, InjectPosition.InvokePosition.AFTER)

fun beforeInvoke(className: String, methodName: String, descriptor: String) =
    InjectPosition.Invoke(className, methodName, descriptor)

fun beforeInvoke(method: KFunction<*>): InjectPosition.Invoke {
    val javaMethod = method.javaMethod ?: error("Failed to get javaMethod of $method")
    return InjectPosition.Invoke(
        javaMethod.declaringClass.name.replace(".", "/"),
        method.name,
        Type.getMethodDescriptor(javaMethod)
    )
}

fun afterInvoke(method: KFunction<*>): InjectPosition.Invoke {
    val javaMethod = method.javaMethod ?: error("Failed to get javaMethod of $method")
    return InjectPosition.Invoke(
        javaMethod.declaringClass.name.replace(".", "/"),
        method.name,
        Type.getMethodDescriptor(javaMethod),
        InjectPosition.InvokePosition.AFTER
    )
}

fun descriptor(returnType: TypeLike, vararg parameterTypes: TypeLike): String =
    Type.getMethodDescriptor(coerceType(returnType), *parameterTypes.map(::coerceType).toTypedArray())

fun <T> injectMethod(
    className: String,
    methodName: String,
    descriptor: String,
    position: InjectPosition = InjectPosition.BeforeAll,
    code: T.(InjectorParams) -> Unit,
) = Injector.inject(className, methodName, descriptor, position, code)

@JvmName("injectMethodNonTyped")
fun injectMethod(
    className: String,
    methodName: String,
    descriptor: String,
    position: InjectPosition = InjectPosition.BeforeAll,
    code: Any.(InjectorParams) -> Unit,
) = Injector.inject(className, methodName, descriptor, position, code)
