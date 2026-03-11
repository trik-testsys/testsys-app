plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

val kotlinVersion = "2.2.21"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
}
