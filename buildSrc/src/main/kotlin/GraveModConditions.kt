import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import java.io.File

/** common's list of grave mods; GraveMods reads the same file at runtime. */
val Project.graveModList: File get() = rootProject.file("common/src/main/resources/postmortem/grave_mods.txt")

private fun Project.graveModIds(): List<String> =
    graveModList.readLines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }

/**
 * Gives the statue collector's recipe a load condition in [loader]'s own format: skip it when any grave mod is loaded.
 * Fabric renamed `any_mod_loaded` to `any_mods_loaded` in 1.20.5, hence [legacyFabric].
 */
fun Project.collectorRecipeConditions(spec: CopySpec, loader: String, legacyFabric: Boolean = false) {
    val ids = graveModIds()
    fun modLoaded(prefix: String) = ids.joinToString(", ") { "{\"type\": \"$prefix:mod_loaded\", \"modid\": \"$it\"}" }
    val conditions = when (loader) {
        "fabric" -> {
            val anyLoaded = if (legacyFabric) "fabric:any_mod_loaded" else "fabric:any_mods_loaded"
            "\"fabric:load_conditions\": [{\"condition\": \"fabric:not\", \"value\": " +
                "{\"condition\": \"$anyLoaded\", \"values\": [${ids.joinToString(", ") { "\"$it\"" }}]}}]"
        }
        "neoforge" -> "\"neoforge:conditions\": [{\"type\": \"neoforge:not\", \"value\": " +
            "{\"type\": \"neoforge:or\", \"values\": [${modLoaded("neoforge")}]}}]"
        "forge" -> "\"conditions\": [{\"type\": \"forge:not\", \"value\": " +
            "{\"type\": \"forge:or\", \"values\": [${modLoaded("forge")}]}}]"
        else -> error("no recipe condition format for $loader")
    }

    spec.filesMatching("data/*/recipe/statue_collector.json") {
        var inserted = false
        filter { line ->
            if (!inserted && line.trim() == "{") {
                inserted = true
                "{\n\t$conditions,"
            } else line
        }
    }
}
