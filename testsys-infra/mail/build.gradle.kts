plugins {
    id("testsys.conventions")
    kotlin("plugin.spring")
}

group = "tech.testsys.infra"

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(project(":testsys-domain"))
    implementation(project(":testsys-infra:localization"))
    implementation(project(":testsys-infra:monitoring"))
    implementation(libs.slf4j.api)
    implementation(libs.spring.context)
    implementation(libs.spring.context.support)
    implementation(libs.spring.tx)
    implementation(libs.spring.boot.core)
    implementation(libs.angus.mail)
    runtimeOnly(libs.kotlin.reflect)
    testImplementation(libs.bundles.test.implementation)
    testRuntimeOnly(libs.bundles.test.runtime)
}
