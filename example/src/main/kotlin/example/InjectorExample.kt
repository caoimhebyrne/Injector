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

package example

import codes.som.anthony.koffee.types.boolean
import codes.som.anthony.koffee.types.long
import codes.som.anthony.koffee.types.void
import dev.cbyrne.injector.Injector
import dev.cbyrne.injector.clazz.loader.InjectorClassLoader
import dev.cbyrne.injector.clazz.transformer.impl.InjectorClassTransformer
import dev.cbyrne.injector.dsl.beforeInvoke
import dev.cbyrne.injector.dsl.beforeReturn
import dev.cbyrne.injector.dsl.descriptor
import dev.cbyrne.injector.dsl.injectMethod
import dev.cbyrne.injector.position.InjectPosition

/**
 * Called from EntryPoint.kt
 */
class InjectorExample {
    fun run() {
        val classLoader = Thread.currentThread().contextClassLoader as InjectorClassLoader
        classLoader.addTransformer(InjectorClassTransformer())

        // Injecting before the existing instructions are executed
        Injector.inject("example/TargetClass", "print", "(Ljava/lang/String;Ljava/lang/String;JJZ)V") {
            println("[InjectorExample] Before all")
        }

        // You can format it using "/" or "."!
        // You can specify a descriptor, the default is "(Ljava/lang/String;Ljava/lang/String;JJZ)V"
        Injector.inject(
            "example.TargetClass",
            "print",
            "(Ljava/lang/String;Ljava/lang/String;JJZ)V",
            InjectPosition.BeforeReturn
        ) {
            println("[InjectorExample] Before return")
        }

        // You can also use the DSL syntax, with this you can reference a function in your invoke position
        injectMethod(
            "example/TargetClass",
            "print",
            "(Ljava/lang/String;Ljava/lang/String;JJZ)V",
            beforeInvoke(System::currentTimeMillis)
        ) {
            println("[InjectorExample] Before invoke System#currentTimeMillis")
        }

        // You can use the DSL syntax to make it easier to construct descriptors!
        val methodDesc = descriptor(void, String::class, String::class, long, long, boolean)

        // You can access parameters from the method you're injecting too!
        injectMethod<TargetClass>(
            "example/TargetClass",
            "print",
            methodDesc
        ) { (params) ->
            println("[InjectorExample] All params: $params")
        }

        // You can replace InjectPosition.Before(All/Return) with a DSL property
        // You can also access fields and methods from this class
        injectMethod<TargetClass>(
            "example/TargetClass",
            "print",
            methodDesc,
            beforeReturn
        ) {
            println("[InjectorExample] Before return using DSL! I can access a field, like $aField")
        }

        // Changing the return value of a method
        injectMethod(
            "example/TargetClass",
            "returnTrue",
            descriptor(boolean)
        ) { (_, returnInfo) ->
            println("[InjectorExample] Overriding return value with false!")
            returnInfo.cancel(false)
        }

        // Testing with a non primitive
        injectMethod(
            "example/TargetClass",
            "nonPrimitive",
            descriptor(String::class)
        ) { (_, returnInfo) ->
            println("[InjectorExample] Overriding return value with custom string!")
            returnInfo.cancel("hello world, it has been overridden")
        }

        // Testing with array
        injectMethod(
            "example/TargetClass",
            "arrayTesting",
            descriptor(List::class)
        ) { (_, returnInfo) ->
            println("[InjectorExample] Overriding return value with custom array!")
            returnInfo.cancel(listOf("oooh", 1, "wow"))
        }

        // Once all injectors are applied, call our method
        TargetClass().print("string parameter")
    }
}
