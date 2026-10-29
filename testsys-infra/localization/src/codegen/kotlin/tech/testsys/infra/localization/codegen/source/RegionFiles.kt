package tech.testsys.infra.localization.codegen.source

import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.fileError

/**
 * The files of one localization resource directory: `regions.properties` and, for every declared region, a directory
 * named by its language tag with one `<bundle>.properties` per bundle. Paths are relative to the resource directory
 * and use `/`; [EXTENSION] is the extension of every file.
 */
internal object RegionFiles {
    const val EXTENSION = ".properties"

    private const val SEPARATOR = '/'
    private val bundleName = Regex("[a-z][a-z0-9_]*")

    /** Reads the regions from [files] (path → content), reporting problems into [errors]. */
    fun loadRegions(files: Map<String, ByteArray>, errors: MutableList<String>): List<RegionDefinition> {
        val bytes = files[RegionsFile.FILE_NAME]
        if (bytes == null) {
            errors += fileError(RegionsFile.FILE_NAME, 1, Problems.Files.REGIONS_FILE_MISSING)
            return emptyList()
        }
        val text = PropertiesScanner.decode(RegionsFile.FILE_NAME, bytes, errors) ?: return emptyList()
        return RegionsFile.parse(text, errors)
    }

    /**
     * Returns the problems of [files] that are not `<region>/<bundle>.properties` of a declared [regions] entry, of
     * declared regions without bundle files and of bundle files that another region has and a region lacks.
     */
    fun fileSetProblems(regions: List<RegionDefinition>, files: Map<String, ByteArray>): List<String> {
        val tags = regions.map { it.languageTag }.toSet()
        val misplaced = files.keys
            .filter { it != RegionsFile.FILE_NAME }
            .mapNotNull { path -> pathProblem(path, tags)?.let { problem -> fileError(path, 1, problem) } }
        val empty = regions
            .filter { region -> bundleFiles(region, files).isEmpty() }
            .map { fileError(RegionsFile.FILE_NAME, it.line, Problems.Files.regionWithoutBundles(it.id, directory(it))) }
        return misplaced + empty + missingBundleProblems(regions, files)
    }

    /**
     * Returns, by region id, the bundles that another region of [regions] has a file for and the region lacks; a
     * region without any bundle files is left out.
     */
    fun missingBundles(regions: List<RegionDefinition>, files: Map<String, ByteArray>): Map<String, Set<String>> {
        val own = regions.associate { region -> region.id to bundlesOf(region, files) }
        val all = own.values.flatten().toSet()
        return own.filterValues { it.isNotEmpty() }
            .mapValues { (_, bundles) -> all - bundles }
            .filterValues { it.isNotEmpty() }
    }

    /**
     * Reads the entries of all bundle files of [region] from [files] in path order, reporting problems into [errors];
     * returns `null` if the region has no bundle files or one of them is not UTF-8.
     */
    fun loadEntries(region: RegionDefinition, files: Map<String, ByteArray>, errors: MutableList<String>): List<PropertiesEntry>? {
        val bundles = bundleFiles(region, files)
        if (bundles.isEmpty()) return null
        val texts = bundles.map { (path, bytes) -> path to PropertiesScanner.decode(path, bytes, errors) }
        if (texts.any { it.second == null }) return null
        return texts.flatMap { (path, text) -> bundleEntries(path, text.orEmpty(), errors) }
    }

    // One problem per missing file, so the keys of the bundle are not reported one by one.
    private fun missingBundleProblems(regions: List<RegionDefinition>, files: Map<String, ByteArray>): List<String> {
        val missing = missingBundles(regions, files)
        return regions.flatMap { region ->
            missing[region.id].orEmpty().sorted().map { bundle ->
                val definedIn = regions.filter { other -> bundle in bundlesOf(other, files) }.joinToString { other -> other.languageTag }
                fileError(directory(region) + bundle + EXTENSION, 1, Problems.Files.missingBundle(bundle, definedIn))
            }
        }
    }

    private fun bundlesOf(region: RegionDefinition, files: Map<String, ByteArray>): Set<String> =
        bundleFiles(region, files).map { (path, _) -> bundleOf(path) }.filter(bundleName::matches).toSet()

    // The keys of a bundle file must start with the bundle of the file.
    private fun bundleEntries(path: String, text: String, errors: MutableList<String>): List<PropertiesEntry> {
        val bundle = bundleOf(path)
        val entries = PropertiesScanner.scan(path, text, errors)
        errors += entries
            .filter { it.key.substringBefore('.') != bundle }
            .map { fileError(path, it.line, Problems.Files.foreignKey(it.key, bundle)) }
        return entries
    }

    private fun pathProblem(path: String, tags: Set<String>): String? {
        val parts = path.split(SEPARATOR)
        return when {
            parts.size != 2 -> Problems.Files.LAYOUT
            parts.first() !in tags -> Problems.Files.undeclaredRegion(parts.first(), RegionsFile.FILE_NAME)
            !bundleName.matches(parts.last().removeSuffix(EXTENSION)) -> Problems.Files.BUNDLE_NAME
            else -> null
        }
    }

    private fun bundleFiles(region: RegionDefinition, files: Map<String, ByteArray>): List<Pair<String, ByteArray>> {
        val inDirectory = files.filterKeys { path -> path.substringBeforeLast(SEPARATOR, missingDelimiterValue = "") == region.languageTag }
        return inDirectory.toSortedMap().map { it.key to it.value }
    }

    private fun bundleOf(path: String): String = path.substringAfter(SEPARATOR).removeSuffix(EXTENSION)

    private fun directory(region: RegionDefinition): String = region.languageTag + SEPARATOR
}
