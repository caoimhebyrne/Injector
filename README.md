# Injector

Injector is a Kotlin / Java library for modifying classes at runtime using ASM

## Installation

```groovy
repositories {
    mavenCentral()
    
    // Required for Koffee, which is a dependency of Injector
    maven("https://maven.hackery.site/")
    
    // Required to retrieve Injector from GitHub
    maven("https://jitpack.io/")
}

dependencies {
    implementation("com.github.cbyrneee:Injector:latest-commit-hash")
}
```

## Usage

**EntryPoint.kt**

```kotlin
fun main(args: Array<String>) {
    val classLoader = InjectorClassLoader()
    Thread.currentThread().contextClassLoader = classLoader

    // Example of invoking your class through the classloader
    val clazz = classLoader.loadClass("Example")
    clazz.getMethod("run").invoke(clazz.getDeclaredConstructor().newInstance())
}
```

**Example.kt**

```kotlin
fun run() {
    val classLoader = Thread.currentThread().contextClassLoader as InjectorClassLoader
    classLoader.addTransformer(InjectorClassTransformer())
       
    injectMethod<Test>("Test", "main", "()V") { (params, fields, returnInfo) -> // this: Test ->
        println("Injecting before the first instruction in Test#main and returning!")
        returnInfo.cancel()
    }
}
```

**For a full example, check out
the [example project](https://github.com/cbyrneee/Injector/tree/main/example/src/main/kotlin/example)**

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please update the example project if making a major change.

## License

[GPL 3.0](https://choosealicense.com/licenses/gpl-3.0/)
