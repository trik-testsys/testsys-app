plugins {
    id("testsys.conventions")
    kotlin("plugin.spring")
}

group = "tech.testsys.infra"

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(project(":testsys-domain"))
    implementation(libs.spring.context)
    implementation(libs.spring.security.crypto)
    testImplementation(libs.bundles.test.implementation)
    testRuntimeOnly(libs.bundles.test.runtime)
}
