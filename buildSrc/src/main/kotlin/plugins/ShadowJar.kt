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

package plugins

object ShadowJar {
    /**
     * Should the shadow jar override the base jarfile
     * or be its own separate jar.
     */
    const val overrideJar = false

    /**
     * Packages which should be moved
     * to another one.
     *
     * Ex:
     * org.ow2.asm -> me.xtrm.project.libs.asm
     */
    val packageRemappings: Map<String, String> =
        mapOf()
}
