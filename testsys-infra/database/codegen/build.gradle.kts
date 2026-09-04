plugins {
    id("testsys.conventions")
}

group = "tech.testsys.infra"

dependencies {
    implementation(project(":testsys-infra:database:codegen-api"))
    // KSP provides symbol-processing-api at runtime; the processor must not bring its own copy.
    compileOnly(libs.ksp.api)
    implementation(libs.kotlinpoet.ksp)

    testImplementation(libs.bundles.test.implementation)
    testImplementation(libs.ksp.api)
    testImplementation(libs.kctfork.ksp)
    testRuntimeOnly(libs.bundles.test.runtime)
}
