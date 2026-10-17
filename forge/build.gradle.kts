plugins {
    id("net.neoforged.moddev.legacyforge") version "2.0.148"
    // Stops several nodes from decompiling Minecraft at the same time.
    id("neoforge-mutex")
}

val node = sc.current.project
val mcVersion = sc.current.version

version = "${property("mod.version")}-forge.$mcVersion"
base.archivesName = property("mod.id") as String

// Forge is only built for 1.20.1, which NeoForge does not cover.
val requiredJava = JavaVersion.VERSION_17

// This branch's own src/ is the default source set; common/ is compiled in alongside it.
addCommonSources(node, sc.current.isActive)

legacyForge {
    version = property("deps.forge") as String

    mods {
        register(property("mod.id") as String) {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        register("client") {
            gameDirectory = rootProject.file("runs/$node-forge")
            client()
        }
        register("server") {
            gameDirectory = rootProject.file("runs/$node-forge")
            server()
        }
    }
}

// Forge 1.20.1 runs on SRG names, so the mixins need a refmap from the annotation processor.
mixin {
    add(sourceSets.main.get(), "postmortem.refmap.json")
    config("postmortem.mixins.json")
}

dependencies {
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
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
        inputs.file(graveModList)

        // The statue collector is only craftable while no grave mod handles deaths.
        collectorRecipeConditions(this, "forge")

        filesMatching("META-INF/mods.toml") { expand(props) }
        legacyRecipeIngredients()
        pluralDataFolders()
        legacyRecipeResults()
        filesMatching("*.mixins.json") {
            expand(props)
            filter { line -> if (line.contains("\"package\"")) "$line\n  \"refmap\": \"postmortem.refmap.json\"," else line }
        }
    }

    // Forge 1.20.1 finds mixin configs through the jar manifest, not mods.toml.
    jar {
        manifest.attributes("MixinConfigs" to "postmortem.mixins.json")
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
