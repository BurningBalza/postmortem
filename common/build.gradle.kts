// common/ is not built on its own: Stonecutter processes its sources for each Minecraft version, and the loader
// branches compile the result (see addCommonSources in fabric/ and neoforge/). Only the java plugin is applied,
// so there is a source set for Stonecutter to process.
plugins {
    java
}

tasks.named { it in setOf("compileJava", "processResources", "classes", "jar") }.configureEach {
    enabled = false
}
