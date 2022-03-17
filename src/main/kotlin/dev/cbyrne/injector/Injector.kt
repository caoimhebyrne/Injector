/*
 *     Injector is a runtime class modification library for Kotlin
 *     Copyright (C) 2021  Conor Byrne
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.cbyrne.injector

import dev.cbyrne.injector.position.InjectPosition
import dev.cbyrne.injector.provider.InjectorParams
import dev.cbyrne.injector.provider.MethodInjector
import dev.cbyrne.injector.transform.InjectorClassTransformer
import fr.stardustenterprises.deface.engine.NativeTransformationService
import net.gudenau.lib.unsafe.Unsafe

object Injector {
    init {
        arrayOf("ReturnInfo", "InjectorParams").forEach { className ->
            ensureLoaded("dev/cbyrne/injector/provider/$className")
        }

        // Initialize the service
        NativeTransformationService.addTransformers(InjectorClassTransformer)
    }

    /**
     * Ensures that the class is force loaded on the bootstrap [ClassLoader]
     */
    private fun ensureLoaded(className: String) {
        val classBytes = Injector::class.java.getResourceAsStream(
            "/$className.class"
        )?.readBytes()
            ?: throw RuntimeException("Couldn't find $className.class")

        Unsafe.defineClass<Any>(
            className,
            classBytes,
            0,
            classBytes.size,
            null,
            null
        )
    }

    @JvmStatic
    fun <T> inject(
        className: String,
        method: String,
        descriptor: String,
        position: InjectPosition = InjectPosition.BeforeAll,
        code: T.(InjectorParams) -> Unit,
    ) = directInject(className, method, descriptor, position, code)

    @JvmStatic
    @JvmName("injectNonTyped")
    fun inject(
        className: String,
        method: String,
        descriptor: String,
        position: InjectPosition = InjectPosition.BeforeAll,
        code: Any.(InjectorParams) -> Unit,
    ) = directInject(className, method, descriptor, position, code)

    private fun <T> directInject(
        className: String,
        method: String,
        descriptor: String,
        position: InjectPosition = InjectPosition.BeforeAll,
        code: T.(InjectorParams) -> Unit
    ) {
        val newClassName = className.replace(".", "/")

        val clazz = NativeTransformationService.findClass(newClassName)
        Unsafe.ensureClassInitialized(clazz)

        InjectorClassTransformer.methodInjectors.add(MethodInjector(newClassName, method, descriptor, position, code))
        NativeTransformationService.retransformClasses(clazz)
    }
}
