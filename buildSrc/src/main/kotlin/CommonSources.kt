import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.getByType

/**
 * Adds common/'s sources for [node] (e.g. "26.2") to this loader node's main source set.
 *
 * The active node reads common/src directly, so the IDE opens and edits the real files. Every other node reads
 * the copy Stonecutter generated for that version, which has its version comments already applied.
 */
fun Project.addCommonSources(node: String, active: Boolean) {
    val common = rootProject.project(":common:$node")
    val generate = "${common.path}:stonecutterGenerate"
    val base = if (active) rootProject.file("common/src/main")
               else common.layout.buildDirectory.dir("generated/stonecutter/main").get().asFile

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
