plugins {
    id("testsys.conventions")
}

dependencies {
    testImplementation(libs.bundles.test.implementation)
    testImplementation(libs.bundles.test.runtime)
}