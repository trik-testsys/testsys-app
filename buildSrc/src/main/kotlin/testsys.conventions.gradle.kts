import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.FailOnSeverity
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("dev.detekt")
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
    detektPlugins(libs.findLibrary("detekt-rules-ktlint-wrapper").get())
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("testAll") {
    group = "verification"
    description = "Runs every test suite, including those registered by modules."
    dependsOn(tasks.withType<Test>())
}

// The plugin-created tasks of every source set inherit these shared settings.
detekt {
    config.setFrom("$rootDir/detekt.yml")
    buildUponDefaultConfig = false
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_4)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_4)

        allWarningsAsErrors = true
        jvmTarget = JvmTarget.JVM_21
    }
}

tasks.withType<Detekt>().configureEach {
    reports {
        sarif.required.set(true)
    }

    // `-Pdetekt.autoCorrect=false` runs Detekt without rewriting sources (read-only checks, e.g. during review).
    autoCorrect = providers.gradleProperty("detekt.autoCorrect").map { it.toBoolean() }.getOrElse(true)
    // detekt 2 dropped `build.maxIssues` (`config.warningsAsErrors` in detekt.yml still exists); failure is now
    // driven by severity instead of an issue count. The project used to fail the build on any finding
    // (`maxIssues: 0`), so fail on the lowest severity (Info) to keep that behaviour.
    failOnSeverity = FailOnSeverity.Info

    // Skip KSP / KAPT / any other build-time generated sources; they are not part of
    // hand-written code and any style nits there are out of the author's control.
    // The String-pattern `exclude("...")` form resolves relative to the source root,
    // which does not include `build/generated/...` in its prefix; use the predicate
    // form against the absolute path instead. `invariantSeparatorsPath` uses `/` on every OS, so the check also works on Windows.
    exclude { it.file.invariantSeparatorsPath.contains("/build/generated/") }
}

tasks.named<Detekt>("detekt") {
    // Compilation tasks (`detekt<Compilation>`) already analyse the sources with their own classpaths; avoid a second pass here.
    setSource(files())
    // `detekt<SourceSet>SourceSet` tasks analyse the same files as the compilation tasks, so they would duplicate findings.
    dependsOn(tasks.withType<Detekt>().matching { task -> task.name != "detekt" && !task.name.endsWith("SourceSet") })
}

tasks.named("check") {
    dependsOn(tasks.named("detekt"))
}
