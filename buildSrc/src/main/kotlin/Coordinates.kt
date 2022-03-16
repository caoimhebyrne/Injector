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

object Coordinates {
    const val NAME = "injector"
    const val DESC = "A JVM library for modifying classes at runtime using ASM."
    const val VENDOR = "cbyrneee"

    const val GIT_HOST = "github.com"
    const val REPO_ID = "cbyrneee/$NAME"

    const val GROUP = "dev.cbyrne"
    const val VERSION = "1.1.0"
}

object Pom {
    val licenses = arrayOf(
        License("GPL-3.0", "https://opensource.org/licenses/GPL-3.0")
    )
    val developers = arrayOf(
        Developer("cbyrneee"),
        Developer("xtrm"),
    )
}

data class License(val name: String, val url: String, val distribution: String = "repo")
data class Developer(val id: String, val name: String = id)
