plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(libs.bundles.kotlin.implementation)
    implementation(libs.bundles.kotlin.spring.plugins)
    implementation(libs.bundles.kotlin.jpa.plugins)
    implementation(libs.bundles.detekt.implementation)
}