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

// This branch's own src/ is the default source set; common/ is compiled in alongside it.
addCommonSources(node, sc.current.isActive)

neoForge {
    version = property("deps.neoforge") as String

    mods {
        register(property("mod.id") as String) {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        register("client") {
            gameDirectory = rootProject.file("runs/$node-neoforge")
            client()
        }
        register("server") {
            gameDirectory = rootProject.file("runs/$node-neoforge")
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
        // Recipe ingredients became plain item ids in 1.21.2.
        if (sc.current.parsed < "1.21.2") legacyRecipeIngredients()
        filesMatching("*.mixins.json") { expand(props) }
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
