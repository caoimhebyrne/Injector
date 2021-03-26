package dev.dreamhopping.example

import dev.dreamhopping.injector.clazz.loader.InjectorClassLoader

fun main(args: Array<String>) {
    val classLoader = InjectorClassLoader()
    Thread.currentThread().contextClassLoader = classLoader

    val clazz = classLoader.loadClass("dev.dreamhopping.example.InjectorExample")
    clazz.getMethod("run").invoke(clazz.getDeclaredConstructor().newInstance())
}
