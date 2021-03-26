# Injector
A side-project to learn about modifying classes at runtime using ASM. A mixin-like library, shouldn't be used in production
It was originally meant to be for a previous project of mine, [PufferfishModLoader](https://github.com/PufferfishModLoader), specifically a part of [PufferfishAPI](https://github.com/PufferfishModLoader/PufferfishAPI) but I decided to make it a stand-alone project for my testing purposes.

## Preparing for injecting
It is advised to change your classloader as early as possible to make sure you can inject into the classes that you wish to. You must also register the ``InjectorClassTransformer`` but you can register your own if you wish.
```kt
val classLoader = Thread.currentThread().contextClassLoader as InjectorClassLoader
classLoader.addTransformer(InjectorClassTransformer())
```

## Using Injector
Once you have your classloader changed, you can start using Injector!
```kt
Injector.inject("dev/dreamhopping/example/Test", "print", InjectPosition.BEFORE_ALL) {
    println("Hello World!")
}

Injector.inject("dev.dreamhopping.example.Test", "print", InjectPosition.AFTER_ALL) {
    println("Goodbye world!")
}

Test().print()
```
