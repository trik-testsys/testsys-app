import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.CommandLineArgumentProvider

plugins {
    id("testsys.conventions")
    idea
}

group = "tech.testsys.infra"

dependencies {
    implementation(libs.bundles.icu.implementation)

    testImplementation(libs.bundles.test.implementation)
    testRuntimeOnly(libs.bundles.test.runtime)
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

// `internal` in Kotlin is per-compilation. Associating codegenTest with codegen gives tests friend-path access to
// internal classes.
kotlin.target.compilations.getByName("codegenTest")
    .associateWith(kotlin.target.compilations.getByName("codegen"))

tasks.register<Test>("codegenTest") {
    group = "verification"
    description = "Runs unit tests for the localization codegen."
    testClassesDirs = codegenTest.output.classesDirs
    classpath = codegenTest.runtimeClasspath
    useJUnitPlatform()
    // The option-honoured tests format java.util.Date operands, which ICU shows in the JVM zone.
    jvmArgs("-Duser.timezone=UTC")
}

tasks.test {
    jvmArgs("-Duser.timezone=UTC")
}

tasks.named("check") {
    dependsOn("codegenTest")
    // The conventions check only the main source set; this module checks the codegen and the tests too.
    dependsOn("detektTest", "detektCodegen", "detektCodegenTest")
}

// A manually created source set is imported by IDEA as production code; mark it as test sources.
idea {
    module {
        testSources.from(codegenTest.kotlin.srcDirs)
        testResources.from(codegenTest.resources.srcDirs)
    }
}

val generatedDir = layout.buildDirectory.dir("generated/source/localization/main/kotlin")
val localizationResources = layout.projectDirectory.dir("src/main/resources/localization")

// Runs the codegen over the resource directory [resources] and writes the API of [packageName] into [outputDir].
fun generateLocalizationTask(
    name: String,
    taskDescription: String,
    resources: Directory,
    outputDir: Provider<Directory>,
    packageName: String,
) = tasks.register<JavaExec>(name) {
    group = "localization"
    description = taskDescription

    classpath = codegen.runtimeClasspath
    mainClass.set("tech.testsys.infra.localization.codegen.MainKt")

    inputs.dir(resources).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.property("packageName", packageName)
    outputs.dir(outputDir)

    argumentProviders.add(
        CommandLineArgumentProvider {
            listOf(
                resources.asFile.absolutePath,
                outputDir.get().asFile.absolutePath,
                packageName,
            )
        }
    )
}

val generateLocalization = generateLocalizationTask(
    name = "generateLocalization",
    taskDescription = "Generate type-safe Localization API from *.properties files.",
    resources = localizationResources,
    outputDir = generatedDir,
    packageName = "tech.testsys.infra.localization",
)

kotlin.sourceSets["main"].kotlin.srcDir(generatedDir)
tasks.named("compileKotlin") { dependsOn(generateLocalization) }

// The example set shows every supported function and construct; the tests render its API, which stays out of the
// product API. It is not a resource root, so its regions file never reaches the test classpath.
val generatedExamplesDir = layout.buildDirectory.dir("generated/source/localization/test/kotlin")
val exampleResources = layout.projectDirectory.dir("src/test/examples/localization")

val generateLocalizationExamples = generateLocalizationTask(
    name = "generateLocalizationExamples",
    taskDescription = "Generate the Localization API of the example set for the tests.",
    resources = exampleResources,
    outputDir = generatedExamplesDir,
    packageName = "tech.testsys.infra.localization.examples",
)

kotlin.sourceSets["test"].kotlin.srcDir(generatedExamplesDir)
tasks.named("compileTestKotlin") { dependsOn(generateLocalizationExamples) }
