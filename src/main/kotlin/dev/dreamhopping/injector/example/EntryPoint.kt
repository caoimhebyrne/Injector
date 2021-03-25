package dev.dreamhopping.injector.example

import dev.dreamhopping.injector.Injector
import dev.dreamhopping.injector.clazz.loader.InjectorClassLoader
import dev.dreamhopping.injector.clazz.transformer.impl.InjectorClassTransformer
import dev.dreamhopping.injector.position.InjectPosition

fun main(args: Array<String>) {
    // Set our current classloader to InjectorClassLoader
    val classLoader = InjectorClassLoader(emptyArray())
    classLoader.addTransformer(InjectorClassTransformer())
    Thread.currentThread().contextClassLoader = classLoader

    Injector().inject("dev/dreamhopping/injector/example/Test", "print", InjectPosition.BEFORE_ALL) {
        println("wow")
    }

    val clazz = classLoader.loadClass("dev.dreamhopping.injector.example.Test")
    clazz.getMethod("print").invoke(clazz.getDeclaredConstructor().newInstance())
}
