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

@file:Suppress("UNUSED_PARAMETER")

package example

class TargetClass {
    val aField = "this is a field from TargetClass, how cool!"
    private val privateField = "this is a private field wow..."
    private val privateInt = 21

    fun print(
        parameter: String,
        anotherParameter: String = "wow another param",
        anotherParameterWow: Long = System.currentTimeMillis(),
        anotherParameterWowWww: Long = System.currentTimeMillis(),
        boolean: Boolean = true
    ) {
        println("[TargetClass] Hello world!")
        println("[TargetClass] returnTrue(): ${returnTrue()}")
        println("[TargetClass] Non primitive value: ${nonPrimitive()}")
        println("[TargetClass] Array value: ${arrayTesting()}")
        println("[TargetClass] Current time: ${System.currentTimeMillis()}")

        // This will not be run as injector will return after currentTimeMillis
        println("[TargetClass] Passed parameter: '$parameter'")
    }

    private fun returnTrue() = true
    private fun nonPrimitive() = "this is not a primitive"
    private fun arrayTesting() = listOf("wow", 0, "crazy")
}
