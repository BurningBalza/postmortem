plugins {
    // Applies the remapping Loom on obfuscated versions and the plain one on 26.1+.
    id("dev.kikugie.loom-back-compat")
}

val node = sc.current.project
val mcVersion = sc.current.version

// DO NOT set group = ...! Stonecutter needs each node's group to stay distinct.
version = "${property("mod.version")}-fabric.$mcVersion"
base.archivesName = property("mod.id") as String

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_17
}

// This branch's own src/ is the default source set; common/ is compiled in alongside it.
// Accessories' API for this node, if Accessories has a build for it; common's accessories bridge comes with it.
val accessories = findProperty("deps.accessories") as String?

addCommonSources(node, sc.current.isActive, listOfNotNull(accessories?.let { "accessories" }))

repositories {
    maven("https://maven.wispforest.io/releases/") { name = "Wisp Forest" }
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    // Mojang official mappings on obfuscated versions, so every node reads the same names as 26.x.
    loomx.applyMojangMappings()

    // `mod*` configurations are converted to plain ones on 26.1+ by loom-back-compat.
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
    // Compile-only: Accessories is optional, and the bridge is only reached once it is found at runtime.
    if (accessories != null) modCompileOnly("io.wispforest:accessories-fabric:$accessories") { isTransitive = false }

    // Backslot, Trinkets, Combat Amenities and Pehkui are all optional at runtime and none is a compile dependency.
    // Each is reached reflectively and stays dormant when absent, so that a missing mod is a
    // feature that does nothing rather than a mod that will not load.
    // See dev.burning.postmortem.compat for the guards.
}

loom {
    fabricModJsonPath = rootProject.file("fabric/src/main/resources/fabric.mod.json")

    runConfigs.all {
        preferGradleTask = true
        // 26.2 Fabric keeps the original run/ (and its world); every other node gets its own,
        // since each needs mods built for its own Minecraft version.
        runDirectory = if (node == "26.2") rootProject.file("run") else rootProject.file("runs/$node-fabric")
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
            "loader" to project.property("mod.fabric_loader_min") as String,
            "java" to requiredJava.majorVersion,
        )
        inputs.properties(props)
        inputs.file(graveModList)

        // The statue collector is only craftable while no grave mod handles deaths.
        collectorRecipeConditions(this, "fabric", legacyFabric = sc.current.parsed < "1.20.5")

        filesMatching("fabric.mod.json") { expand(props) }
        // Recipe ingredients became plain item ids in 1.21.2.
        if (sc.current.parsed < "1.21.2") legacyRecipeIngredients()
        // Data-pack folders went singular in 1.21.
        if (sc.current.parsed < "1.21") pluralDataFolders()
        if (sc.current.parsed < "1.20.5") legacyRecipeResults()
        filesMatching("*.mixins.json") { expand(props) }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds this node's jar and copies it to the root build/libs/."
        from(loomx.modJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("libs"))
    }
}
