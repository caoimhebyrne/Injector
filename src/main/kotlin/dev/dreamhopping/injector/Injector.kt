package dev.dreamhopping.injector

import dev.dreamhopping.injector.position.InjectPosition
import dev.dreamhopping.injector.provider.MethodInjector

object Injector {
    val methodInjectors = mutableListOf<MethodInjector>()

    fun inject(className: String, method: String, position: InjectPosition, code: () -> Unit) {
        methodInjectors.add(MethodInjector(className.replace(".", "/"), method, position, code))
    }

    @JvmStatic
    fun callMethodInjectors(className: String, method: String, position: String) {
        val parsedPos = InjectPosition.fromString(position) ?: throw IllegalStateException("Invalid InjectPosition")

        methodInjectors.filter {
            it.className == className && it.method == method && it.position == parsedPos
        }.forEach {
            it.code()
            methodInjectors.remove(it)
        }
    }
}
