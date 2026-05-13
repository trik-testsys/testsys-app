import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    id("io.gitlab.arturbosch.detekt")
}

group = "tech.testsys"
version = "1.0.0-SNAPSHOT"

val libs = versionCatalogs.named("libs")

repositories {
    mavenCentral()
}

// Accessing libs.versions.toml from buildSrc is complicated, so avoid declaring dependencies here.
dependencies {
    testImplementation(kotlin("test"))
    detektPlugins(libs.findLibrary("detekt-formatting").get())
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)

        allWarningsAsErrors = true
        jvmTarget = JvmTarget.JVM_21
    }
}

tasks.withType<Detekt>().configureEach {
    reports {
        sarif.required.set(true)
    }

    config.setFrom("$rootDir/detekt.yml")
    buildUponDefaultConfig = false
    autoCorrect = true

    // Skip KSP / KAPT / any other build-time generated sources; they are not part of
    // hand-written code and any style nits there are out of the author's control.
    // The String-pattern `exclude("...")` form resolves relative to the source root,
    // which does not include `build/generated/...` in its prefix; use the predicate
    // form against the absolute path instead.
    exclude { it.file.absolutePath.contains("/build/generated/") }
}

tasks.named("detekt") {
    enabled = false
}

tasks.named("check") {
    dependsOn(tasks.named("detektMain"))
}