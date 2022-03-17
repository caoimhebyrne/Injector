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

package example.java;

import dev.cbyrne.injector.Injector;
import dev.cbyrne.injector.position.InjectPosition;
import dev.cbyrne.injector.provider.InjectorParams;
import example.TargetClass;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;

/**
 * The Java syntax is still very much WIP, but
 * can be considered as in a usable state.
 *
 * This is more of an example as to why you *shouldn't*
 * use it, though it can be useful in specific situations.
 *
 * @author xtrm-en
 */
public class InjectorExample {
    // We cannot use lambda expressions when
    // declaring hooks in Java.
    public static void main(String[] args) {
        //noinspection Convert2Lambda
        Injector.inject(
                "example/TargetClass",
                "print",
                "(Ljava/lang/String;Ljava/lang/String;JJZ)V",
                InjectPosition.BeforeAll.INSTANCE,
                new Function2<>() {
                    @Override
                    public Unit invoke(Object instance, InjectorParams injectorParams) {
                        System.out.println("[InjectorExample] Before all");
                        return Unit.INSTANCE;
                    }
                }
        );

        //noinspection Convert2Lambda
        Injector.inject(
                "example/TargetClass",
                "print",
                "(Ljava/lang/String;Ljava/lang/String;JJZ)V",
                InjectPosition.BeforeTail.INSTANCE,
                new Function2<>() {
                    @Override
                    public Unit invoke(Object instance, InjectorParams injectorParams) {
                        System.out.println("[InjectorExample] Before tail");
                        return Unit.INSTANCE;
                    }
                }
        );

        new TargetClass().print(
                "string parameter",
                "wow another param",
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                true
        );
    }
}
