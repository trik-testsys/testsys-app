package tech.testsys.infra.localization.codegen

import com.squareup.kotlinpoet.FileSpec
import org.junit.jupiter.api.Assertions.assertThrows

/** Complete `ru` variants of an integer selector: its categories are one, few and many; `other` needs fractions. */
internal const val INTEGER_PLURAL_VARIANTS: String = "one {{a}} few {{b}} many {{c}} * {{d}}"

/** A Russian `glossary.task` term with the cases `nom` and `acc`. */
internal const val RU_TASK_TERM: String = "glossary.task=.input {\$case :string} .input {\$form :string} .match \$case \$form " +
    "nom one {{Задача}} nom few {{Задачи}} nom many {{Задач}} nom other {{Задачи}} nom sg {{Задача}} nom pl {{Задачи}} " +
    "acc one {{Задачу}} acc few {{Задачи}} acc many {{Задач}} acc other {{Задачи}} acc sg {{Задачу}} acc pl {{Задачи}} " +
    "* * {{Задача}}"

/** In-memory resource directory: path → UTF-8 content. */
internal fun files(vararg entries: Pair<String, String>): Map<String, ByteArray> =
    entries.associate { (path, content) -> path to content.toByteArray(Charsets.UTF_8) }

/** Splits the entries [lines] of the region [tag] into its bundle files `<tag>/<bundle>.properties` by key bundle. */
internal fun bundleFiles(tag: String, lines: List<String>): List<Pair<String, String>> = lines
    .groupBy { it.substringBefore('=').substringBefore('.') }
    .map { (bundle, entries) -> "$tag/$bundle.properties" to entries.joinToString("\n") }

/** Runs the codegen over [entries] and returns the generated files. */
internal fun generate(vararg entries: Pair<String, String>): List<FileSpec> = LocalizationCodegen.generate(files(*entries))

/** Runs the codegen over [files], which must fail, and returns the reported errors. */
internal fun errorsOf(files: Map<String, ByteArray>): List<String> =
    assertThrows(LocalizationCodegenException::class.java) { LocalizationCodegen.generate(files) }.errors

/** Returns the files of the only region RU whose bundle files hold the entries [lines]. */
internal fun ruFiles(vararg lines: String): Array<Pair<String, String>> =
    arrayOf("regions.properties" to "RU=ru-RU\n", *bundleFiles("ru-RU", lines.toList()).toTypedArray())

/** Returns the files of the regions RU and EN whose bundle files hold the entries [ru] and [en]. */
internal fun ruEnFiles(ru: List<String>, en: List<String>): Array<Pair<String, String>> = arrayOf(
    "regions.properties" to "RU=ru-RU\nEN=en-US\n",
    *bundleFiles("ru-RU", ru).toTypedArray(),
    *bundleFiles("en-US", en).toTypedArray(),
)

/** Returns the errors of a run with the only region RU whose bundle files hold the entries [lines]. */
internal fun ruErrors(vararg lines: String): List<String> = errorsOf(files(*ruFiles(*lines)))

/** Returns the errors of a run with regions RU and EN. */
internal fun ruEnErrors(ru: List<String>, en: List<String>): List<String> = errorsOf(files(*ruEnFiles(ru, en)))

/** Returns the source text of the generated file [name] for regions RU and EN. */
internal fun ruEnSource(ru: List<String>, en: List<String>, name: String): String =
    generate(*ruEnFiles(ru, en)).single { it.name == name }.toString()
