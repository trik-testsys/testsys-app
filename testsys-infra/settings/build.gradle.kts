plugins {
    id("testsys.conventions")
    kotlin("plugin.spring")
}

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(project(":testsys-domain"))
    implementation(project(":testsys-infra:monitoring"))
    implementation(libs.spring.context)
    testImplementation(libs.bundles.test.implementation)
    testRuntimeOnly(libs.bundles.test.runtime)
}
