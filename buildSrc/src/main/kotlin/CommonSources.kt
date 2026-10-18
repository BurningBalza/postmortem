import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.getByType

/**
 * Adds common/'s sources for [node] (e.g. "26.2") to this loader node's main source set.
 *
 * The active node reads common/src directly, so the IDE opens and edits the real files. Every other node reads
 * the copy Stonecutter generated for that version, which has its version comments already applied.
 */
fun Project.addCommonSources(node: String, active: Boolean, extra: List<String> = emptyList()) {
    val common = rootProject.project(":common:$node")

    // "main" plus any of common's bridge source sets this loader takes (curios, accessories).
    for (set in listOf("main") + extra) {
        val generate = "${common.path}:stonecutterGenerate" + if (set == "main") "" else set.replaceFirstChar { it.uppercase() }
        val base = if (active) rootProject.file("common/src/$set")
                   else common.layout.buildDirectory.dir("generated/stonecutter/$set").get().asFile

        extensions.getByType<SourceSetContainer>().named("main") {
            java.srcDir(base.resolve("java"))
            resources.srcDir(base.resolve("resources"))
        }

        if (!active) {
            tasks.named { it == "compileJava" || it == "processResources" || it == "sourcesJar" }.configureEach {
                dependsOn(generate)
            }
        }
    }
}
