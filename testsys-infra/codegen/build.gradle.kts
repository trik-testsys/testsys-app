plugins {
    id("testsys.conventions")
}

group = "tech.testsys.infra"

dependencies {
    implementation(project(":testsys-infra:codegen-api"))
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet.ksp)

    testImplementation(libs.bundles.test.implementation)
    testImplementation(libs.kctfork.ksp)
    testRuntimeOnly(libs.bundles.test.runtime)
}
