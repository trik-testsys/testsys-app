import java.security.MessageDigest
import java.util.HexFormat

plugins {
    id("testsys.conventions")
}

group = "tech.testsys.web"

dependencies {
    api(platform(libs.vaadin.bom))
    api(libs.vaadin.core)
    api(project(":testsys-infra:localization"))
    implementation(libs.bundles.icu.implementation)

    testImplementation(libs.bundles.test.implementation)
    testImplementation(libs.karibu.testing)
    testRuntimeOnly(libs.bundles.test.runtime)
}

// Vaadin uses resource timestamps for HTTP cache revalidation.
tasks.jar { isPreserveFileTimestamps = true }

// Versions each token import of styles.css by the content hash of the imported file: Vaadin versions only the
// stylesheet itself, so without it a changed token file stays in the browser cache for an hour after a deploy.
val themeTokens = layout.projectDirectory.dir("src/main/resources/META-INF/resources/testsys-ui/tokens")
tasks.processResources {
    inputs.dir(themeTokens)
    val tokens = themeTokens.asFile
    filesMatching("META-INF/resources/testsys-ui/styles/styles.css") {
        filter { line ->
            Regex("""@import url\('\.\./tokens/([\w-]+\.css)'\);""").replace(line) { match ->
                val file = match.groupValues[1]
                val digest = MessageDigest.getInstance("SHA-256").digest(tokens.resolve(file).readBytes())
                val hash = HexFormat.of().formatHex(digest, 0, 4)
                "@import url('../tokens/$file?v-c=$hash');"
            }
        }
    }
}

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
