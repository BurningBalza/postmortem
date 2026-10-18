// common/ is not built on its own: Stonecutter processes its sources for each Minecraft version, and the loader
// branches compile the result (see addCommonSources in fabric/ and neoforge/). Only the java plugin is applied,
// so there is a source set for Stonecutter to process.
plugins {
    java
}

// Bridges to other mods' slot APIs, compiled only into the loaders those mods exist on (see addCommonSources).
sourceSets {
    create("curios")
    create("accessories")
}

tasks.named { it.startsWith("compile") || it.startsWith("process") || it.endsWith("classes", ignoreCase = true) || it == "jar" }
    .configureEach { enabled = false }
