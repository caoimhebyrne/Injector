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
 *
 * This is a simple Proof Of Concept, you're gonna need
 * a bit more engineering to make something useful out of this,
 * aka allow your classes to be called from the bootclassloader.
 *
 * Doing this in Java is necessary because the Kotlin compiler
 * will try sticking his <code>Intrinsics</code> calls everywhere,
 * and it gets quite messy really quick.
 *
 * @author xtrm 
 */
public class InternalExample {
    public static void main(String[] args) throws MalformedURLException {
        //noinspection Convert2Lambda
        Injector.<URL>inject(
                "java/net/URL",
                "<init>",
                InjectorDslKt.descriptor(Type.VOID_TYPE, URL.class, String.class, URLStreamHandler.class),
                InjectPosition.BeforeAll.INSTANCE,
                new Function2<>() {
                    @Override
                    public Unit invoke(URL instance, InjectorParams injectorParams) {
                        System.out.println("URL Created: " + injectorParams.getParams().get(1));
                        return Unit.INSTANCE;
                    }
                }
        );
        
        new URL("https://github.com/xtrm-en");
    }
}
