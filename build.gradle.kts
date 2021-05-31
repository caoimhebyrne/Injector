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

plugins {
    kotlin("jvm") version "1.5.10"
    id("maven-publish")
}

group = "dev.cbyrne.injector"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://maven.hackery.site/")
    maven("https://jitpack.io/")
}

dependencies {
    implementation(kotlin("stdlib"))

    api(kotlin("reflect"))
    api("org.ow2.asm:asm-commons:9.1")

    api("codes.som.anthony:koffee:8.0.2") {
        exclude(module = "asm")
        exclude(module = "asm-commons")
        exclude(module = "asm-tree")
    }

    api("com.github.videogame-hacker:koffee-kotlin-reflect-sugar:9821b76eee") {
        exclude(module = "asm")
        exclude(module = "asm-commons")
        exclude(module = "asm-tree")
        exclude(module = "kotlin-reflect")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
