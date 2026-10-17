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
