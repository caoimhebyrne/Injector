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
import java.net.URL
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

plugins {
    with(Plugins) {
        // Language Plugins
        `java-library`
        kotlin("jvm") version KOTLIN

        // Git Repo Information
        id("org.ajoberstar.grgit") version GRGIT

        // Code Quality
        id("org.jlleitschuh.gradle.ktlint") version KTLINT

        // Documentation Generation
        id("org.jetbrains.dokka") version DOKKA

        // Maven Publication
        id("io.github.gradle-nexus.publish-plugin") version NEXUS_PUBLISH
        `maven-publish`
        signing
    }
}

group = Coordinates.GROUP
version = Coordinates.VERSION

// What JVM version should this project compile to
val targetVersion = "1.8"
// What JVM version this project is written in
val sourceVersion = "1.8"
// Should we generate an /api/ source set
val apiSourceSet = false

// Maven Repositories
repositories {
    mavenLocal()
    mavenCentral()

    Repositories.mavenUrls.forEach(::maven)
}

// Project Dependencies
dependencies {
    with(Dependencies) {
        implementation("org.jetbrains.kotlin", "kotlin-stdlib", KOTLIN)
        api("org.jetbrains.kotlin", "kotlin-reflect", KOTLIN)

        arrayOf("asm", "asm-tree").forEach { module ->
            api("org.ow2.asm", module, ASM)
        }

        implementation("fr.stardustenterprises", "unsafe", UNSAFE)
        implementation("fr.stardustenterprises", "deface", DEFACE)

        api("codes.som.anthony", "koffee", KOFFEE) {
            exclude(group = "org.ow2.asm")
            exclude(group = "org.jetbrains.kotlin")
        }
    }
}

// Generate the /api/ source set
if (apiSourceSet) {
    sourceSets {
        val name = "api"

        val main by sourceSets
        val test by sourceSets

        val sourceSet = create(name) {
            java.srcDir("src/$name/kotlin")
            resources.srcDir("src/$name/resources")

            this.compileClasspath += main.compileClasspath
            this.runtimeClasspath += main.runtimeClasspath
        }

        arrayOf(main, test).forEach {
            it.compileClasspath += sourceSet.output
            it.runtimeClasspath += sourceSet.output
        }
    }
}

// Disable unneeded rules
ktlint {
    this.disabledRules.add("no-wildcard-imports")
}

tasks {
    test {
        useJUnitPlatform()
    }

    // Configure JVM versions
    compileKotlin {
        kotlinOptions.jvmTarget = targetVersion
    }
    compileJava {
        targetCompatibility = targetVersion
        sourceCompatibility = sourceVersion
    }

    dokkaHtml {
        val moduleFile = File(projectDir, "MODULE.temp.md")

        run {
            // In order to have a description on the rendered docs, we have to have
            // a file with the # Module thingy in it. That's what we're
            // automagically creating here.

            doFirst {
                moduleFile.writeText("# Module ${Coordinates.NAME}\n${Coordinates.DESC}")
            }

            doLast {
                moduleFile.delete()
            }
        }

        moduleName.set(Coordinates.NAME)

        dokkaSourceSets.configureEach {
            displayName.set("${Coordinates.NAME} on ${Coordinates.GIT_HOST}")
            includes.from(moduleFile.path)

            skipDeprecated.set(false)
            includeNonPublic.set(false)
            skipEmptyPackages.set(true)
            reportUndocumented.set(true)
            suppressObviousFunctions.set(true)

            // Link the source to the documentation
            sourceLink {
                localDirectory.set(file("src"))
                remoteUrl.set(URL("https://${Coordinates.GIT_HOST}/${Coordinates.REPO_ID}/tree/trunk/src"))
            }

            // External documentation link template
//            externalDocumentationLink {
//                url.set(URL("https://javadoc.io/doc/net.java.dev.jna/jna/5.10.0/"))
//            }
        }
    }

    // The original artifact, we just have to add the API source output and the
    // LICENSE file.
    jar {
        fun normalizeVersion(versionLiteral: String): String {
            val regex = Regex("(\\d+\\.\\d+\\.\\d+).*")
            val match = regex.matchEntire(versionLiteral)
            require(match != null) {
                "Version '$versionLiteral' does not match version pattern, e.g. 1.0.0-QUALIFIER"
            }
            return match.groupValues[1]
        }

        val buildTimeAndDate = OffsetDateTime.now()
        val buildDate = DateTimeFormatter.ISO_LOCAL_DATE.format(buildTimeAndDate)
        val buildTime = DateTimeFormatter.ofPattern("HH:mm:ss.SSSZ").format(buildTimeAndDate)

        val javaVersion = System.getProperty("java.version")
        val javaVendor = System.getProperty("java.vendor")
        val javaVmVersion = System.getProperty("java.vm.version")

        mapOf(
            "Created-By" to "$javaVersion ($javaVendor $javaVmVersion)",
            "Build-Date" to buildDate,
            "Build-Time" to buildTime,
            "Build-Revision" to grgit.log()[0].id,
            "Specification-Title" to project.name,
            "Specification-Version" to normalizeVersion(project.version.toString()),
            "Specification-Vendor" to Coordinates.VENDOR,
            "Implementation-Title" to Coordinates.NAME,
            "Implementation-Version" to Coordinates.VERSION,
            "Implementation-Vendor" to Coordinates.VENDOR,
            "Bundle-Name" to Coordinates.NAME,
            "Bundle-Description" to Coordinates.DESC,
            "Bundle-DocURL" to "https://${Coordinates.GIT_HOST}/${Coordinates.REPO_ID}",
            "Bundle-Vendor" to Coordinates.VENDOR,
            "Bundle-SymbolicName" to Coordinates.GROUP + '.' + Coordinates.NAME
        ).forEach { (k, v) ->
            manifest.attributes[k] = v
        }

        if (apiSourceSet) {
            from(sourceSets["api"].output)
        }
        from("LICENSE")
    }

    if (apiSourceSet) {
        // API artifact, only including the output of the API source and the
        // LICENSE file.
        create("apiJar", Jar::class) {
            group = "build"

            archiveClassifier.set("api")
            from(sourceSets["api"].output)

            from("LICENSE")
        }
    }

    // Source artifact, including everything the 'main' does but not compiled.
    create("sourcesJar", Jar::class) {
        group = "build"

        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
        if (apiSourceSet) {
            from(sourceSets["api"].allSource)
        }

        from("LICENSE")
    }

    // The Javadoc artifact, containing the Dokka output and the LICENSE file.
    create("javadocJar", Jar::class) {
        group = "build"

        val dokkaHtml = getByName("dokkaHtml")

        archiveClassifier.set("javadoc")
        dependsOn(dokkaHtml)
        from(dokkaHtml)

        from("LICENSE")
    }
}

// Define the default artifacts' tasks
val defaultArtifactTasks = arrayOf(
    tasks["sourcesJar"],
    tasks["javadocJar"]
).also {
    if (apiSourceSet) {
        it.plus(tasks["apiJar"])
    }
}

// Declare the artifacts
artifacts {
    defaultArtifactTasks.forEach(::archives)
}

publishing.publications {
    // Sets up the Maven integration.
    create("mavenJava", MavenPublication::class.java) {
        from(components["java"])
        defaultArtifactTasks.forEach(::artifact)

        with(Coordinates) {
            pom {
                name.set(NAME)
                description.set(DESC)
                url.set("https://$GIT_HOST/$REPO_ID")

                with(Pom) {
                    licenses {
                        licenses.forEach {
                            license {
                                name.set(it.name)
                                url.set(it.url)
                                distribution.set(it.distribution)
                            }
                        }
                    }

                    developers {
                        developers.forEach {
                            developer {
                                id.set(it.id)
                                name.set(it.name)
                            }
                        }
                    }
                }

                scm {
                    connection.set("scm:git:git://$GIT_HOST/$REPO_ID.git")
                    developerConnection.set("scm:git:ssh://$GIT_HOST/$REPO_ID.git")
                    url.set("https://$GIT_HOST/$REPO_ID")
                }
            }
        }

        // Configure the signing extension to sign this Maven artifact.
        signing.sign(this)
    }
}

// Configure publishing to Maven Central
nexusPublishing.repositories.sonatype {
    nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
    snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))

    // Skip this step if environment variables NEXUS_USERNAME or NEXUS_PASSWORD aren't set.
    username.set(properties["NEXUS_USERNAME"] as? String ?: return@sonatype)
    password.set(properties["NEXUS_PASSWORD"] as? String ?: return@sonatype)
}

// Task priority
tasks.getByName("closeAndReleaseSonatypeStagingRepository")
    .mustRunAfter("publishToSonatype")

// Wrapper task since calling both one after the other
// in IntellIJ seems to cause some problems.
tasks.create("releaseToSonatype") {
    this.dependsOn(
        "publishToSonatype",
        "closeAndReleaseSonatypeStagingRepository"
    )
    this.group = "publishing"
}
