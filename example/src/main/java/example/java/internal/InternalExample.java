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

package example.java.internal;

import dev.cbyrne.injector.Injector;
import dev.cbyrne.injector.dsl.InjectorDslKt;
import dev.cbyrne.injector.position.InjectPosition;
import dev.cbyrne.injector.provider.InjectorParams;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import org.objectweb.asm.Type;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;

/**
 * This is where it gets spicy. 🔥
 * <p>
 * This is a simple Proof Of Concept, you're gonna need
 * a bit more engineering to make something useful out of this,
 * aka allow your classes to be called from the bootclassloader.
 * <p>
 * Doing this in Java is necessary because the Kotlin compiler
 * will try sticking his <code>Intrinsics</code> calls everywhere,
 * and it gets quite messy really quick.
 *
 * @author xtrm
 */
public class InternalExample {
    public static void main(String[] args) throws MalformedURLException {
        // noinspection Convert2Lambda
        Injector.inject(
                "java/net/URL",
                "<init>",
                InjectorDslKt.descriptor(Type.VOID_TYPE, URL.class, String.class, URLStreamHandler.class),
                InjectPosition.BeforeAll.INSTANCE,
                true,
                false,
                new Function2<URL, InjectorParams, Unit>() {
                    @Override
                    public Unit invoke(URL instance, InjectorParams injectorParams) {
                        String url = (String) injectorParams.getParams().get(1);

                        // You can't directly call this method since its class,
                        // example.java.internal.InternalExample
                        // is defined on the System ClassLoader, and URL is
                        // defined on a higher classloader in the hierarchy,
                        // so it doesn't have access to its children's classes.
                        try {
                            catchURL(url);
                        } catch (NoClassDefFoundError ignored) {
                        }

                        // To combat this behavior, you could use Reflection
                        // to get the class from another ClassLoader
                        // (the system classloader for instance) and then
                        // call the method.
                        try {
                            Class.forName("example.java.internal.InternalExample", true, ClassLoader.getSystemClassLoader())
                                    .getDeclaredMethod("catchURL", String.class)
                                    .invoke(null, url);
                        } catch (Throwable ignored) {
                        }

                        return Unit.INSTANCE;
                    }
                }
        );

        new URL("https://github.com/xtrm-en");
    }

    public static void catchURL(String param) {
        System.out.println("Caught URL: " + param);
    }
}
