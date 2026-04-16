plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(libs.bundles.kotlin.implementation)
    implementation(libs.bundles.detekt.implementation)
}