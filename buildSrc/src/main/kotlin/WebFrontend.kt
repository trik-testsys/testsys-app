import org.gradle.api.Project

/**
 * Builds the production frontend only for runs that package the application.
 *
 * @since %CURRENT_VERSION%
 */
fun Project.configureWebFrontend() {
    gradle.taskGraph.whenReady {
        val packaged = hasTask("${project.path}:bootJar") || hasTask("${project.path}:bootBuildImage")
        tasks.named("vaadinBuildFrontend") { enabled = packaged }
    }
}
