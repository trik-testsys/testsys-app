import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("testsys.conventions")
}

group = "tech.testsys.domain"

dependencies {
    implementation(project(":testsys-domain"))
    testImplementation(libs.bundles.test.implementation)
    testRuntimeOnly(libs.bundles.test.runtime)
}

