package dev.dreamhopping.injector.clazz.loader

import dev.dreamhopping.injector.clazz.transformer.IClassTransformer
import java.net.URL
import java.net.URLClassLoader

class InjectorClassLoader(urls: Array<URL>) : URLClassLoader(urls) {
    private val transformers = mutableListOf<IClassTransformer>()
    private val exclusions = mutableListOf("java.", "sun.", "javax.", "argo.", "org.objectweb.asm.")

    override fun loadClass(name: String): Class<*> {
        if (exclusions.any { name.startsWith(it) }) return super.loadClass(name)

        val pathName = name.replace(".", "/")
        val resource = getResource("$pathName.class") ?: throw ClassNotFoundException()
        var bytes = resource.openStream().readAllBytes()

        transformers.forEach { bytes = it.transformClass(pathName, bytes) }
        return defineClass(name, bytes, 0, bytes.size)
    }

    fun addTransformer(transformer: IClassTransformer) = transformers.add(transformer)
    fun addExclusion(exclusion: String) = exclusions.add(exclusion)
}
