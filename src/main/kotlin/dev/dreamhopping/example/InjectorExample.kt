package dev.dreamhopping.example

import dev.dreamhopping.injector.Injector
import dev.dreamhopping.injector.clazz.loader.InjectorClassLoader
import dev.dreamhopping.injector.clazz.transformer.impl.InjectorClassTransformer
import dev.dreamhopping.injector.position.InjectPosition

class InjectorExample {
    fun run() {
        val classLoader = Thread.currentThread().contextClassLoader as InjectorClassLoader
        classLoader.addTransformer(InjectorClassTransformer())

        Injector.inject("dev/dreamhopping/example/Test", "print", InjectPosition.BEFORE_ALL) {
            println("wow before all...")
        }

        Injector.inject("dev/dreamhopping/example/Test", "print", InjectPosition.BEFORE_RETURN) {
            println("wow before return...")
        }

        Injector.inject("dev/dreamhopping/example/Test", "print", InjectPosition.BEFORE_RETURN) {
            println("you can have multiple at one position...")
        }

        Test().print()
    }
}
