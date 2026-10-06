plugins {
    id("testsys.conventions")
    kotlin("plugin.spring")
}

group = "tech.testsys.infra"

dependencies {
    implementation(project(":testsys-domain"))
    implementation(libs.ksoup)
    implementation(libs.spring.context)
    testImplementation(libs.bundles.test.implementation)
    testRuntimeOnly(libs.bundles.test.runtime)
}
