pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "FabricMC" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    // Picks the remapping Loom for obfuscated versions and the plain one for 26.1+.
    id("dev.kikugie.loom-back-compat") version "0.4.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        // common/ holds the shared code and only version-processes it; each loader branch compiles common's
        // sources for its Minecraft version alongside its own. Nodes are named after the Minecraft line and
        // compile against the exact release given as the second value.
        val versions = listOf("1.21.4" to "1.21.4", "1.21.11" to "1.21.11", "26.1" to "26.1.2", "26.2" to "26.2", "26.3" to "26.3")

        for (loader in listOf("common", "fabric", "neoforge")) branch(loader) {
            for ((name, release) in versions) version(name, release)
        }

        vcsVersion = "26.2"
    }
}

rootProject.name = "postmortem"
