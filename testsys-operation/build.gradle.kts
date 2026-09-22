plugins {
    id("testsys.conventions")
}

group = "tech.testsys.operation"

dependencies {
    implementation(project(":testsys-domain"))
    testImplementation(libs.bundles.test.implementation)
    testRuntimeOnly(libs.bundles.test.runtime)
}

