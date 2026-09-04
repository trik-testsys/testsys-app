plugins {
    id("testsys.conventions")
    alias(libs.plugins.ksp)
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
}

group = "tech.testsys.infra"

dependencies {
    implementation(project(":testsys-domain"))
    implementation(project(":testsys-infra:database:codegen-api"))
    implementation(libs.bundles.database.implementation)
    runtimeOnly(libs.postgresql)

    ksp(project(":testsys-infra:database:codegen"))

    testImplementation(libs.bundles.test.implementation)
    testImplementation(libs.bundles.database.test.implementation)
    testRuntimeOnly(libs.bundles.test.runtime)
}
