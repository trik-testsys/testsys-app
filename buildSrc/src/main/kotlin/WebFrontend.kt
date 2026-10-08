import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.jvm.tasks.Jar

/**
 * Configures each web application's frontend bundle and its own production token.
 *
 * @since %CURRENT_VERSION%
 */
fun Project.configureWebFrontend() {
    gradle.taskGraph.whenReady {
        val packaged = hasTask("${project.path}:bootJar") || hasTask("${project.path}:bootBuildImage")
        tasks.named("vaadinBuildFrontend") { enabled = packaged }
    }
    tasks.named("bootJar", Jar::class.java) {
        from(layout.buildDirectory.file("cached-flow-build-info.json")) {
            into("META-INF/VAADIN/config")
            rename { "flow-build-info.json" }
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }
}
