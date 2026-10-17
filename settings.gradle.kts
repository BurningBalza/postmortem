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
        val versions = listOf(
            "1.20.4" to "1.20.4", "1.20.5" to "1.20.5", "1.20.6" to "1.20.6", "1.21" to "1.21", "1.21.1" to "1.21.1", "1.21.2" to "1.21.2", "1.21.3" to "1.21.3", "1.21.4" to "1.21.4", "1.21.5" to "1.21.5", "1.21.6" to "1.21.6", "1.21.7" to "1.21.7",
            "1.21.8" to "1.21.8", "1.21.9" to "1.21.9", "1.21.10" to "1.21.10", "1.21.11" to "1.21.11",
            "26.1" to "26.1.2", "26.2" to "26.2", "26.3" to "26.3",
        )
        // NeoForge only builds where it had a stable release; its jar for the next release up also declares the
        // versions it skipped (e.g. 1.21.8 covers 1.21.6 and 1.21.7).
        val neoforgeSkips = setOf("1.20.5", "1.21.2", "1.21.6", "1.21.7", "1.21.9")

        branch("common") { for ((name, release) in versions) version(name, release) }
        branch("fabric") { for ((name, release) in versions) version(name, release) }
        branch("neoforge") { for ((name, release) in versions) if (name !in neoforgeSkips) version(name, release) }

        vcsVersion = "26.3"
    }
}

rootProject.name = "postmortem"
