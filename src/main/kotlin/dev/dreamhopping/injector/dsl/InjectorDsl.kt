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

package dev.dreamhopping.injector.dsl

import dev.dreamhopping.injector.Injector
import dev.dreamhopping.injector.position.InjectPosition
import dev.dreamhopping.injector.provider.MethodInjector
import org.objectweb.asm.Type
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

val beforeAll = InjectPosition.BeforeAll
val beforeReturn = InjectPosition.BeforeReturn

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

fun <T> injectMethod(
    className: String,
    methodName: String,
    descriptor: String,
    position: InjectPosition = InjectPosition.BeforeAll,
    code: T.(List<Any>) -> Unit
) {
    Injector.methodInjectors.add(
        MethodInjector(
            className,
            methodName,
            descriptor,
            position,
            code
        )
    )
}

@JvmName("injectMethodNonTyped")
fun injectMethod(
    className: String,
    methodName: String,
    descriptor: String,
    position: InjectPosition = InjectPosition.BeforeAll,
    code: Any.(List<Any>) -> Unit
) {
    Injector.methodInjectors.add(
        MethodInjector(
            className,
            methodName,
            descriptor,
            position,
            code
        )
    )
}
