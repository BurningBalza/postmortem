plugins {
    id("net.neoforged.moddev") version "2.0.148"
    // Stops several nodes from decompiling Minecraft at the same time.
    id("neoforge-mutex")
}

val node = sc.current.project
val mcVersion = sc.current.version

version = "${property("mod.version")}-neoforge.$mcVersion"
base.archivesName = property("mod.id") as String

val requiredJava = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_17
}

neoForge {
    version = property("deps.neoforge") as String

    mods {
        register(property("mod.id") as String) {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        register("client") {
            gameDirectory = rootProject.file("runs/$node")
            client()
        }
        register("server") {
            gameDirectory = rootProject.file("runs/$node")
            server()
        }
    }
}

java {
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    processResources {
        val props = mapOf(
            "version" to project.version.toString(),
            "minecraft" to project.property("mod.mc_compat") as String,
            "java" to requiredJava.majorVersion,
        )
        inputs.properties(props)

        filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
        filesMatching("*.mixins.json") { expand(props) }

        exclude("fabric.mod.json", "META-INF/mods.toml")
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds this node's jar and copies it to the root build/libs/."
        from(jar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("libs"))
    }
}
