plugins {
    id("testsys.conventions")
    kotlin("plugin.spring")
}

group = "tech.testsys.infra"

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(project(":testsys-domain"))
    implementation(project(":testsys-infra:settings"))
    implementation(libs.ksoup)
    implementation(libs.spring.context)
    testImplementation(libs.bundles.test.implementation)
    testRuntimeOnly(libs.bundles.test.runtime)
}
