package tech.testsys.infra.localization.codegen

import tech.testsys.infra.localization.codegen.emitter.KotlinEmitter
import tech.testsys.infra.localization.codegen.parser.SignatureMerger
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.Properties

/**
 * Loads every `*.properties` file in a directory as one bundle per region.
 *
 * The file's base name is taken as the region id (e.g. `RU.properties` → `"RU"`),
 * which must line up with the supported regions known at runtime.
 *
 * UTF-8 is enforced explicitly: [Properties.load] defaults to ISO-8859-1 when given
 * an [java.io.InputStream], which would silently mangle non-ASCII patterns.
 */
internal class PropertiesBundleLoader {

    fun load(resourceDir: File): Map<String, Map<String, String>> {
        if (!resourceDir.isDirectory) return emptyMap()
        val files = resourceDir.listFiles { _, name -> name.endsWith(".properties") }.orEmpty()
        // Sorted so codegen output (and any error messages) are deterministic across filesystems.
        return files.associateTo(sortedMapOf()) { file -> file.nameWithoutExtension to readProps(file) }
    }

    private fun readProps(file: File): Map<String, String> {
        val props = Properties()
        file.inputStream().use { it.reader(StandardCharsets.UTF_8).use(props::load) }
        return props.entries.associate { (k, v) -> k.toString() to v.toString() }
    }
}

/**
 * CLI entry point invoked by the `generateLocalization` Gradle task with [args] `<resourceDir> <outputDir>`.
 *
 * @since %CURRENT_VERSION%
 */
fun main(args: Array<String>) {
    require(args.size == 2) { "Usage: <resourceDir> <outputDir>" }
    val resourceDir = File(args[0])
    val outputDir = File(args[1])

    // Wiped and recreated so stale generated sources from removed keys never linger and confuse compileKotlin.
    outputDir.deleteRecursively()
    outputDir.mkdirs()

    val perRegion = PropertiesBundleLoader().load(resourceDir)
    if (perRegion.isEmpty()) {
        println("No localization bundles found in $resourceDir; skipping codegen.")
        return
    }
    SignatureMerger().merge(perRegion)
        .let(KotlinEmitter()::emit)
        .forEach { it.writeTo(outputDir) }
}
