import org.gradle.api.file.CopySpec

private val INGREDIENT = Regex("""^(\s*"[^"]{1}"\s*:\s*)"(#?)([^"]+)"(,?)\s*$""")

/**
 * Rewrites recipe keys back to the pre-1.21.2 ingredient form: `"S": "minecraft:stone"` becomes
 * `"S": { "item": "minecraft:stone" }`, and a `#tag` becomes `{ "tag": ... }`. The stored recipes use the
 * newer plain-id form; JSON has no comments for Stonecutter to switch.
 */
fun CopySpec.legacyRecipeIngredients() {
    filesMatching("data/*/recipe/*.json") {
        filter { line ->
            INGREDIENT.matchEntire(line)?.let { m ->
                val (key, tag, id, comma) = m.destructured
                "$key{ \"${if (tag.isEmpty()) "item" else "tag"}\": \"$id\" }$comma"
            } ?: line
        }
    }
}

private val SINGULAR_FOLDER = Regex(
    "^data/([^/]+)/(recipe|loot_table|advancement|structure|tags/item|tags/block|tags/entity_type|tags/fluid|tags/game_event)/")

/** Moves data-pack folders back to their pre-1.21 plural names: `recipe/` to `recipes/`, `tags/item/` to `tags/items/`. */
fun CopySpec.pluralDataFolders() {
    includeEmptyDirs = false
    eachFile {
        path = SINGULAR_FOLDER.replace(path) { m -> "data/${m.groupValues[1]}/${m.groupValues[2]}s/" }
    }
}

private val RESULT_ID = Regex("""^(\s*)"id"(\s*:)""")

/** A recipe result names its item with `"item"` before 1.20.5, not `"id"`; `"id"` only appears in results. */
fun CopySpec.legacyRecipeResults() {
    filesMatching("data/*/recipe/*.json") {
        filter { line -> RESULT_ID.replace(line) { m -> "${m.groupValues[1]}\"item\"${m.groupValues[2]}" } }
    }
}
