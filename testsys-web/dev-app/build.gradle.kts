plugins {
    id("testsys.conventions")
    kotlin("plugin.spring")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.vaadin)
}

group = "tech.testsys.web"

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(platform(libs.vaadin.bom))
    implementation(project(":testsys-web:components"))
    implementation(project(":testsys-infra:localization"))
    implementation(libs.vaadin.spring.boot.starter)
    implementation(libs.kotlin.reflect)
    developmentOnly(platform(libs.vaadin.bom))
    developmentOnly(libs.vaadin.dev)

    testImplementation(libs.bundles.test.implementation)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.karibu.testing)
    testImplementation(libs.karibu.testing.spring)
    testRuntimeOnly(libs.bundles.test.runtime)
}

// Vaadin 25.2–25.3 turns production mode on whenever the project has a bootJar task, so it also builds the production
// frontend before bootRun and tests. Only a jar or an image needs the production bundle.
gradle.taskGraph.whenReady {
    val packaged = hasTask("${project.path}:bootJar") || hasTask("${project.path}:bootBuildImage")
    tasks.named("vaadinBuildFrontend") { enabled = packaged }
}

// Vaadin 25.3 registers one production-token service for the whole Gradle build. With two applications,
// that service restores only the first application's token. Package each application's own cached token.
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    from(layout.buildDirectory.file("cached-flow-build-info.json")) {
        into("META-INF/VAADIN/config")
        rename { "flow-build-info.json" }
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}
