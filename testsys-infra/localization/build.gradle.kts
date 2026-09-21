import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.CommandLineArgumentProvider

plugins {
    id("testsys.conventions")
    idea
}

group = "tech.testsys.infra"

dependencies {
    implementation(libs.bundles.icu.implementation)
}

val codegen by sourceSets.creating
val codegenTest by sourceSets.creating

dependencies {
    "codegenImplementation"(libs.icu4j)
    "codegenImplementation"(libs.kotlinpoet.core)

    "codegenTestImplementation"(codegen.output)
    "codegenTestImplementation"(codegen.runtimeClasspath)
    "codegenTestImplementation"(libs.bundles.test.implementation)
    "codegenTestRuntimeOnly"(libs.bundles.test.runtime)
}

// `internal` in Kotlin is per-compilation. Associating codegenTest with codegen
// gives tests friend-path access to internal classes (MessagePatternAnalyzer etc.).
kotlin.target.compilations.getByName("codegenTest")
    .associateWith(kotlin.target.compilations.getByName("codegen"))

tasks.register<Test>("codegenTest") {
    group = "verification"
    description = "Runs unit tests for the localization codegen."
    testClassesDirs = codegenTest.output.classesDirs
    classpath = codegenTest.runtimeClasspath
    useJUnitPlatform()
}

tasks.named("check") { dependsOn("codegenTest") }

// A manually created source set is imported by IDEA as production code; mark it as test sources.
idea {
    module {
        testSources.from(codegenTest.kotlin.srcDirs)
        testResources.from(codegenTest.resources.srcDirs)
    }
}

val generatedDir = layout.buildDirectory.dir("generated/source/localization/main/kotlin")
val localizationResources = layout.projectDirectory.dir("src/main/resources/localization")

val generateLocalization = tasks.register<JavaExec>("generateLocalization") {
    group = "localization"
    description = "Generate type-safe Localization API from *.properties files."

    classpath = codegen.runtimeClasspath
    mainClass.set("tech.testsys.infra.localization.codegen.MainKt")

    inputs.dir(localizationResources).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(generatedDir)

    argumentProviders.add(
        CommandLineArgumentProvider {
            listOf(
                localizationResources.asFile.absolutePath,
                generatedDir.get().asFile.absolutePath,
            )
        }
    )
}

kotlin.sourceSets["main"].kotlin.srcDir(generatedDir)
tasks.named("compileKotlin") { dependsOn(generateLocalization) }
