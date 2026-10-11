plugins {
    id("testsys.conventions")
}

group = "tech.testsys.web"

dependencies {
    api(platform(libs.vaadin.bom))
    api(libs.vaadin.core)

    testImplementation(libs.bundles.test.implementation)
    testImplementation(libs.karibu.testing)
    testRuntimeOnly(libs.bundles.test.runtime)
}

// Vaadin uses resource timestamps for HTTP cache revalidation.
tasks.jar { isPreserveFileTimestamps = true }

val guideVersionSource = layout.buildDirectory.dir("generated/source/guide/main/kotlin")
val generateGuideVersion by tasks.registering {
    val version = libs.versions.driver.js
    inputs.property("driverVersion", version)
    outputs.dir(guideVersionSource)
    doLast {
        val output = guideVersionSource.get().file("tech/testsys/web/components/overlay/GuideVersion.kt").asFile
        output.parentFile.mkdirs()
        output.writeText("package tech.testsys.web.components.overlay\n\ninternal const val DRIVER_VERSION = \"${version.get()}\"\n")
    }
}
kotlin.sourceSets.main { kotlin.srcDir(guideVersionSource) }
tasks.named("compileKotlin") { dependsOn(generateGuideVersion) }
