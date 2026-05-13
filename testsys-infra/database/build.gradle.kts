plugins {
    id("testsys.conventions")
    alias(libs.plugins.ksp)
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
}

group = "tech.testsys.infra"

dependencies {
    implementation(project(":testsys-domain"))
    implementation(project(":testsys-infra:codegen-api"))
    implementation(libs.bundles.database.implementation)
    runtimeOnly(libs.postgresql)

    ksp(project(":testsys-infra:codegen"))

    testImplementation(libs.bundles.test.implementation)
    testImplementation(libs.bundles.database.test.implementation)
    testRuntimeOnly(libs.bundles.test.runtime)
}
