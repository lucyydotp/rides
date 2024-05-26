plugins {
    kotlin("jvm") version "2.0.0"
    id("io.papermc.paperweight.userdev") version "1.7.1"
    id("xyz.jpenilla.run-paper") version "2.2.4"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "me.lucyydotp"
version = "1.0.0-SNAPSHOT"

kotlin {
    jvmToolchain(21)
    compilerOptions {
        explicitApi()
    }
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

val shade by configurations.creating
configurations.implementation.configure { extendsFrom(shade) }
tasks.shadowJar { configurations = listOf(shade) }

dependencies {
    shade(kotlin("stdlib-jdk8"))
    paperweight.paperDevBundle("1.20.6-R0.1-SNAPSHOT")
}


