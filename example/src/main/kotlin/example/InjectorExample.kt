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

import dev.dreamhopping.injector.Injector
import dev.dreamhopping.injector.clazz.loader.InjectorClassLoader
import dev.dreamhopping.injector.clazz.transformer.impl.InjectorClassTransformer
import dev.dreamhopping.injector.dsl.afterInvoke
import dev.dreamhopping.injector.dsl.beforeInvoke
import dev.dreamhopping.injector.dsl.beforeReturn
import dev.dreamhopping.injector.dsl.injectMethod
import dev.dreamhopping.injector.position.InjectPosition

/**
 * Called from EntryPoint.kt
 */
class InjectorExample {
    fun run() {
        val classLoader = Thread.currentThread().contextClassLoader as InjectorClassLoader
        classLoader.addTransformer(InjectorClassTransformer())

        // Injecting before the existing instructions are executed
        Injector.inject("example/TargetClass", "print", "()V") {
            println("[InjectorExample] Before all")
        }

        // You can format it using "/" or "."!
        // You can specify a descriptor, the default is "()V"
        Injector.inject("example.TargetClass", "print", "()V", InjectPosition.BeforeReturn) {
            println("[InjectorExample] Before return")
        }

        // You can also use the DSL syntax, with this you can reference a function in your invoke position
        injectMethod("example/TargetClass", "print", "()V", beforeInvoke(System::currentTimeMillis)) {
            println("[InjectorExample] Before invoke System#currentTimeMillis")
        }

        // You can replace InjectPosition.Before(All/Return) with a DSL property
        // You can also access fields and methods from this class
        injectMethod<TargetClass>("example/TargetClass", "print", "()V", beforeReturn) {
            println("[InjectorExample] Before return using DSL! I can access a field, like $aField")
        }

        // Injecting after PrintStream#println has been invoked
        injectMethod(
            "example/TargetClass",
            "print",
            "()V",
            afterInvoke("java/io/PrintStream", "println", "(Ljava/lang/Object;)V")
        ) {
            println("[InjectorExample] After invoke PrintStream#println")
        }

        // Once all injectors are applied, call our method
        TargetClass().print()
    }
}
