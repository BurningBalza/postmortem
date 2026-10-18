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

// Curios' API for this node, if Curios has a build for it; common's curios bridge is only compiled in with it.
val curios = findProperty("deps.curios") as String?
// Likewise Accessories, with common's accessories bridge.
val accessories = findProperty("deps.accessories") as String?

// This branch's own src/ is the default source set; common/ is compiled in alongside it.
addCommonSources(node, sc.current.isActive, listOfNotNull(curios?.let { "curios" }, accessories?.let { "accessories" }))

repositories {
    maven("https://maven.theillusivec4.top/") { name = "Curios" }
    maven("https://maven.wispforest.io/releases/") { name = "Wisp Forest" }
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
            gameDirectory = rootProject.file("runs/$node-neoforge")
            client()
        }
        register("server") {
            gameDirectory = rootProject.file("runs/$node-neoforge")
            server()
        }
    }
}

dependencies {
    // Compile-only: Curios is optional, and the bridge is only reached once it is found at runtime.
    if (curios != null) compileOnly("top.theillusivec4.curios:curios-neoforge:$curios:api")
    if (accessories != null) compileOnly("io.wispforest:accessories-neoforge:$accessories") { isTransitive = false }
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
        collectorRecipeConditions(this, "neoforge")

        filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
        // Recipe ingredients became plain item ids in 1.21.2.
        if (sc.current.parsed < "1.21.2") legacyRecipeIngredients()
        // Data-pack folders went singular in 1.21.
        if (sc.current.parsed < "1.21") pluralDataFolders()
        if (sc.current.parsed < "1.20.5") legacyRecipeResults()
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
