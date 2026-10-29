plugins {
    id("testsys.conventions")
    kotlin("plugin.spring")
}

dependencies {
    implementation(platform(libs.spring.boot.bom))
    api(libs.micrometer.core)
    implementation(libs.spring.context)
    testImplementation(libs.bundles.test.implementation)
    testRuntimeOnly(libs.bundles.test.runtime)
}
