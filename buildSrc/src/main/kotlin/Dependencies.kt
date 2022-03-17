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

private const val kotlinVersion = "1.6.10"

object Plugins {
    const val KOTLIN = kotlinVersion
    const val GRGIT = "4.1.1" // old version for jgit to work on Java 8
    const val BLOSSOM = "1.3.0"
    const val SHADOW = "7.1.2"
    const val KTLINT = "10.2.1"
    const val DOKKA = kotlinVersion
    const val NEXUS_PUBLISH = "1.0.0"
}

object Dependencies {
    const val KOTLIN = kotlinVersion
    const val ASM = "9.2"
    const val UNSAFE = "1.7.2"
    const val DEFACE = "0.2.0"
    const val KOFFEE = "8.0.2"
}

object Repositories {
    val mavenUrls = arrayOf(
        "https://maven.hackery.site/"
    )
}
