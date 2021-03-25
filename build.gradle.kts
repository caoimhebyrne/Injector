plugins {
    kotlin("jvm") version "1.4.31"
}

group = "dev.dreamhopping"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val asmVersion = "9.1"
dependencies {
    implementation(kotlin("stdlib"))

    api("org.ow2.asm:asm:$asmVersion")
    api("org.ow2.asm:asm-commons:$asmVersion")
}
