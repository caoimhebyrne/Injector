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

**Example.kt**

```kotlin
fun run() {
    injectMethod<Test>("Test", "main", "()V") { (params, fields, returnInfo) -> // this: Test ->
        println("Injecting before the first instruction in Test#main and returning!")
        returnInfo.cancel()
    }
    
    Test().main() // will simply return
}
```

**For a full example, check out
the [example project](https://github.com/cbyrneee/Injector/tree/main/example/src/main/kotlin/example/kotlin)**

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please update the example project if making a major change.

## License

[GPL 3.0](https://choosealicense.com/licenses/gpl-3.0/)
