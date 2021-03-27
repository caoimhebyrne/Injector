# Injector

A side-project to learn about modifying classes at runtime using ASM. A mixin-like library, shouldn't be used in
production.

It was originally meant to be for a project of mine, [PufferfishModLoader](https://github.com/PufferfishModLoader),
specifically a part of [PufferfishAPI](https://github.com/PufferfishModLoader/PufferfishAPI) but I decided to make it a
stand-alone project for my testing purposes.

## Using Injector

### Preparing for injecting

It is advised to change your classloader as early as possible to make sure you can inject into the classes that you wish
to.

``EntryPoint.kt``

```kt
fun main(args: Array<String>) {
    val classLoader = InjectorClassLoader()
    Thread.currentThread().contextClassLoader = classLoader

    // Example of invoking your class through the classloader
    val clazz = classLoader.loadClass("Example")
    clazz.getMethod("run").invoke(clazz.getDeclaredConstructor().newInstance())
}
```

Now, you can register the ``InjectorClassLoader`` in your class that was called from the entry point.

``Example#run``

```kt
fun run() {
    val classLoader = Thread.currentThread().contextClassLoader as InjectorClassLoader
    classLoader.addTransformer(InjectorClassTransformer())
       
    ...
}
```

### Injecting into a target class

Once you have your classloader changed, you can start using Injector!

**Our test class**

```kt
class Test {
    val aNumberField = 0
    
    fun print() {
        println("Time in millis: ${System.currentTimeMills()}")
    }
}
```

**Using the Injector class**

```kt
Injector.inject("dev/dreamhopping/example/Test", "print", "()V", InjectPosition.BeforeAll) {
    println("Hello World!")
}

Injector.inject("dev/dreamhopping/example/Test", "print", "()V", InjectPosition.BeforeReturn) {
    println("Goodbye World!")
}
```

**Using the Kotlin DSL (more powerful)**

```kt
injectMethod("dev/dreamhopping/example/Test", "print", "()V") {
    println("Hello World!")
}

injectMethod("dev/dreamhopping/example/Test", "print", "()V", beforeReturn) {
    println("Goodbye World!")
}
```

You can even access fields and methods from your target class via Injector!

```kt
injectMethod<Test>("dev/dreamhopping/example/Test", "print", "()V") { // this: Test
    println("Here, have a field from the target class: $aNumberField")
}
```

If you want to insert after or before an invocation of a certain method, you can also do that!

```kt
injectMethod("dev/dreamhopping/example/Test", "print", "()V", afterInvoke("java/io/PrintStream", "println", "(Ljava/lang/Object;)V")) {
    println("After println!")
}

// You can also reference the method without typing out the descriptor and owner fully!
injectMethod("dev/dreamhopping/example/Test", "print", "()V", afterInvoke(System::currentTimeMillis)) {
    println("After currentTimeMillis!")
}
```

**Example Injector Output**

When your class is modified at runtime using the injectors above, this is a simplified version of what it will look like
in Kotlin code.

If you're wondering why the methods seem to be "out of order" that's because the name follows the
format``injectorMethod{x}``. ``x`` is the array index of the injector, this is **not class specific** and **not in order
of injection point**.

```kt
class Test {
    val aNumberField = 0
    
    fun print() {
        // Anything starting with injectorMethod{x} 
        // is an injector reference
        injectorMethod0()
        injectorMethod2()

        // Note: currentTimeMillis is not stored in a val, 
        // this is just easier for illustration purposes
        val currentTimeMillis = System.currentTimeMills()
        injectorMethod4()

        println("Time in millis: ${currentTimeMillis}")
        injectorMethod3()
        injectorMethod1()
    }
    
    fun injectorMethod0() {
        println("Hello World!")
    }
    
    fun injectorMethod1() {
        println("Goodbye World!")
    }
    
    fun injectorMethod2() {
        println("Here, have a field from the target class: $aNumberField")
    }
    
    fun injectorMethod3() {
        println("After println!")
    }
    
    fun injectorMethod4() {
        println("After currentTimeMillis!")
    } 
}
```

### Other Injector Features

**Accessing parameters from the target method**

With Injector, you can access the parameters of the target method when injecting.

*Note: If you attempt to change a parameter, it will not change them in the target method*

``Calculator.kt``

```kt
class Calculator {
    fun add(a: Int, b: Int): Int {
        return a + b
    }
}
```

``EntryPoint.kt``

```kt
fun main(args: Array<String>) {
    // Injector setup omitted, see the previous section
    
    injectMethod<Calculator>("Calculator", "add", "(II)I") { params -> // this: Calculator
        val a = params.getOrNull(0) as? Int ?: return
        val b = params.getOrNull(1) as? Int ?: return
        
        println("Adding $a and $b!")
    }
    
    Calculator.add(1, 1)
}
```

Simplified output of ``Calculator.kt``

*Note: ``listOf`` is not actually called like this, it's just a simplification to make this easy to read, but the
concept is the same*

```kt
class Calculator {
    fun add(a: Int, b: Int): Int {
        injectorMethod0(this, listOf(a, b))
        return a + b
    }
    
    fun injectorMethod0(obj: Calculator, params: List<Object>) {
        val a = params.getOrNull(0) as? Int ?: return
        val b = params.getOrNull(1) as? Int ?: return
        
        println("Adding $a and $b!")
    }
}
```