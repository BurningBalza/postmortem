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
        // Each "{name}-{loader}" node lives in versions/ and builds with build.{loader}.gradle.kts.
        // [version] is the exact Minecraft release compiled against when it differs from the node's name.
        fun match(name: String, vararg loaders: String, version: String = name) {
            for (loader in loaders) version("$name-$loader", version).buildscript("build.$loader.gradle.kts")
        }

        match("26.1", "fabric", "neoforge", version = "26.1.2")
        match("26.2", "fabric", "neoforge")
        vcsVersion = "26.2-fabric"
    }
}

rootProject.name = "postmortem"
