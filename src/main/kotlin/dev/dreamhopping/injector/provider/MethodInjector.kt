package dev.dreamhopping.injector.provider

import dev.dreamhopping.injector.position.InjectPosition

data class MethodInjector(val className: String, val method: String, val position: InjectPosition, val code: () -> Unit)
