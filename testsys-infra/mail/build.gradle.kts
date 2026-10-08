plugins {
    id("testsys.conventions")
    kotlin("plugin.spring")
}

group = "tech.testsys.infra"

dependencies {
    implementation(project(":testsys-domain"))
    implementation(project(":testsys-infra:localization"))
    implementation(libs.spring.context)
    implementation(libs.spring.context.support)
    implementation(libs.spring.tx)
    implementation(libs.angus.mail)
    testImplementation(libs.bundles.test.implementation)
    testRuntimeOnly(libs.bundles.test.runtime)
}
