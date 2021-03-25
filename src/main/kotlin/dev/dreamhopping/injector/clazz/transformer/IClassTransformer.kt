package dev.dreamhopping.injector.clazz.transformer

interface IClassTransformer {
    fun transformClass(name: String, classBytes: ByteArray): ByteArray
}