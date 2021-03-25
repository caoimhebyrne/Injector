package dev.dreamhopping.injector

import dev.dreamhopping.injector.clazz.transformer.impl.InjectorClassTransformer
import dev.dreamhopping.injector.position.InjectPosition
import dev.dreamhopping.injector.provider.MethodInjector

class Injector {
    fun inject(className: String, method: String, position: InjectPosition, code: () -> Unit) =
        InjectorClassTransformer.methodInjectors.add(MethodInjector(className.replace(".", "/"), method, position, code))

    fun test() {
        val methodInjector = MethodInjector("test", "test", InjectPosition.BEFORE_ALL) {
            println("test")
        }

        methodInjector.code()
    }

    companion object {
        fun callLambda(methodInjector: MethodInjector) {
            methodInjector.code()
        }
    }
}
