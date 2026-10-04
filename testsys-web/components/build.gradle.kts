plugins {
    id("testsys.conventions")
}

group = "tech.testsys.web"

dependencies {
    api(platform(libs.vaadin.bom))
    api(libs.vaadin.core)
    implementation(project(":testsys-infra:localization"))
    implementation(libs.bundles.icu.implementation)

    testImplementation(libs.bundles.test.implementation)
    testImplementation(libs.karibu.testing)
    testRuntimeOnly(libs.bundles.test.runtime)
}

// Vaadin uses resource timestamps for HTTP cache revalidation.
tasks.jar { isPreserveFileTimestamps = true }
