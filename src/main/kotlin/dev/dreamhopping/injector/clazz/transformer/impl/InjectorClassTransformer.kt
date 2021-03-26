package dev.dreamhopping.injector.clazz.transformer.impl

import dev.dreamhopping.injector.Injector
import dev.dreamhopping.injector.clazz.transformer.IClassTransformer
import dev.dreamhopping.injector.position.InjectPosition
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*

class InjectorClassTransformer : IClassTransformer {
    override fun transformClass(name: String, classBytes: ByteArray): ByteArray {
        // Check if any injectors exist for this class
        println("[InjectorClassTransformer] methodInjectors: ${Injector.methodInjectors}")
        val applicableMethodInjectors = Injector.methodInjectors.filter { it.className == name }
        if (applicableMethodInjectors.isEmpty()) return classBytes

        // Load the class bytes into a class node
        val classReader = ClassReader(classBytes)
        val classNode = ClassNode()
        classReader.accept(classNode, ClassReader.EXPAND_FRAMES)

        // Apply method injectors
        applicableMethodInjectors.forEach { injector ->
            val method = classNode.methods.firstOrNull { it.name == injector.method } ?: return@forEach
            val list = InsnList()
            list.add(LdcInsnNode(classNode.name))
            list.add(LdcInsnNode(method.name))
            list.add(LdcInsnNode(injector.position.name))
            list.add(
                MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "dev/dreamhopping/injector/Injector",
                    "callMethodInjectors",
                    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                    false
                )
            )
            when (injector.position) {
                InjectPosition.BEFORE_ALL -> {
                    method.instructions.insert(list)
                }

                InjectPosition.BEFORE_RETURN -> {
                    method.instructions.insertBefore(method.instructions.last.previous, list)
                }
            }
        }

        // Write the transformed class
        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
        classNode.accept(classWriter)

        return classWriter.toByteArray()
    }
}
