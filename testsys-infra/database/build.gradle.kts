plugins {
    id("testsys.conventions")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
}

group = "tech.testsys.infra"

dependencies {
    implementation(project(":testsys-domain"))
    implementation(libs.bundles.database.implementation)
    runtimeOnly(libs.postgresql)

    testImplementation(libs.bundles.test.implementation)
    testImplementation(libs.bundles.database.test.implementation)
    testRuntimeOnly(libs.bundles.test.runtime)
}
