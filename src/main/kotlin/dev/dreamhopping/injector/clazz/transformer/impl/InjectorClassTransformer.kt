package dev.dreamhopping.injector.clazz.transformer.impl

import dev.dreamhopping.injector.clazz.transformer.IClassTransformer
import dev.dreamhopping.injector.position.InjectPosition
import dev.dreamhopping.injector.provider.MethodInjector
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*

class InjectorClassTransformer : IClassTransformer {
    companion object {
        val methodInjectors = mutableListOf<MethodInjector>()
    }

    override fun transformClass(name: String, classBytes: ByteArray): ByteArray {
        // Check if any injectors exist for this class
        val applicableMethodInjectors = methodInjectors.filter { it.className == name }
        if (applicableMethodInjectors.isEmpty()) return classBytes

        // Load the class bytes into a class node
        val classReader = ClassReader(classBytes)
        val classNode = ClassNode()
        classReader.accept(classNode, ClassReader.EXPAND_FRAMES)

        // Apply method injectors
        applicableMethodInjectors.forEach { injector ->
            val applicableMethod = classNode.methods.firstOrNull { it.name == injector.method } ?: return@forEach

            when (injector.position) {
                InjectPosition.BEFORE_ALL -> {
                    val list = InsnList()
                    list.add(
                        MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "dev/dreamhopping/injector/Injector",
                            "callLambda",
                            "(Ldev/dreamhopping/injector/provider/MethodInjector;)V"
                        )
                    )

                    applicableMethod.instructions.insert(list)
                }
            }
        }

        // Write the transformed class
        val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        classNode.accept(classWriter)

        return classWriter.toByteArray()
    }
}
