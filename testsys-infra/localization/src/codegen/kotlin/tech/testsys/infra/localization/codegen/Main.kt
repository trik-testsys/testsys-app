package tech.testsys.infra.localization.codegen

import tech.testsys.infra.localization.codegen.source.RegionFiles
import java.io.File
import kotlin.system.exitProcess

private const val RESOURCE_DIR = 0
private const val OUTPUT_DIR = 1
private const val PACKAGE = 2
private const val ARGUMENTS = 3

/**
 * CLI entry point invoked by the localization generation Gradle tasks with [args] `<resourceDir> <outputDir>
 * <package>`: the API of the resource directory is generated into the package.
 *
 * @since %CURRENT_VERSION%
 */
fun main(args: Array<String>) {
    require(args.size == ARGUMENTS) { "Expected the arguments <resourceDir> <outputDir> <package>, got ${args.size}" }
    val resourceDir = File(args[RESOURCE_DIR])
    val outputDir = File(args[OUTPUT_DIR])
    val packageName = args[PACKAGE]

    // Wiped and recreated so stale generated sources from removed keys never linger and confuse compileKotlin.
    outputDir.deleteRecursively()
    outputDir.mkdirs()

    val files = resourceDir.walkTopDown()
        .filter { file -> file.isFile && file.name.endsWith(RegionFiles.EXTENSION) }
        .associateTo(sortedMapOf()) { it.relativeTo(resourceDir).invariantSeparatorsPath to it.readBytes() }
    try {
        LocalizationCodegen.generate(files, packageName).forEach { it.writeTo(outputDir) }
    } catch (e: LocalizationCodegenException) {
        System.err.println(e.message)
        exitProcess(1)
    }
}
