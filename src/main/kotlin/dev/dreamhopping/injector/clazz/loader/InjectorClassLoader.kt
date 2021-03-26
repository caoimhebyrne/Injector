package dev.dreamhopping.injector.clazz.loader

import dev.dreamhopping.injector.clazz.transformer.impl.InjectorClassTransformer
import java.net.URLClassLoader

class InjectorClassLoader : URLClassLoader(emptyArray(), null) {
    private val transformers = mutableListOf<InjectorClassTransformer>()
    private val exclusions =
        mutableListOf("java.", "kotlin.", "sun.", "javax.", "argo.", "org.objectweb.asm.", "dev.dreamhopping.injector.")

    override fun loadClass(name: String): Class<*> {
        if (exclusions.any { name.startsWith(it) }) return javaClass.classLoader.loadClass(name)

        val pathName = name.replace(".", "/")
        val resource = getResource("$pathName.class") ?: javaClass.classLoader.getResource("$pathName.class")
        ?: throw ClassNotFoundException()
        var bytes = resource.openStream().readAllBytes()

        transformers.forEach { bytes = it.transformClass(pathName, bytes) }
        return defineClass(name, bytes, 0, bytes.size)
    }

    fun addTransformer(transformer: InjectorClassTransformer) = transformers.add(transformer)
    fun addExclusion(exclusion: String) = exclusions.add(exclusion)
}
