package tech.testsys.infra.localization.codegen.emitter

import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.messageError
import tech.testsys.infra.localization.codegen.mf2.validator.term.Glossary
import tech.testsys.infra.localization.codegen.signature.MergedKey
import tech.testsys.infra.localization.codegen.signature.Placeholder.SelectPlaceholder

/**
 * Checks the Kotlin names the keys produce: key shape, clashes after normalization, reserved names and nested
 * enum collisions. Class and nested enum names are compared ignoring case: on a case-insensitive file system names
 * that differ only in case share one file.
 */
internal object NameValidator {
    private val keyShape = Regex("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+")
    private val reservedMethodNames = setOf("toString", "hashCode", "equals")
    private val reservedTypeNames = GeneratedTypes.reservedTypeNames.map { it.lowercase() }.toSet()

    /** Returns the problems of the names of [keys]; each is reported for the region [regionOf] returns for its key. */
    fun findProblems(keys: List<MergedKey>, regionOf: (String) -> String): List<String> {
        val messages = keys.filterNot { it.key.startsWith(Glossary.GLOSSARY_PREFIX) }
        val (shaped, malformed) = messages.partition { keyShape.matches(it.key) }
        val shapeProblems = malformed.map { messageError(regionOf(it.key), it.key, Problems.Names.KEY_SHAPE) }
        val classProblems = shaped.groupBy { Naming.classNameFor(it.key).lowercase() }.flatMap { (_, sameFile) ->
            listOfNotNull(classProblem(sameFile, regionOf)) +
                sameFile.groupBy { Naming.classNameFor(it.key) }.flatMap { (className, group) ->
                    methodProblems(className, group, regionOf) + enumProblems(className, group, regionOf)
                }
        }
        return shapeProblems + classProblems
    }

    private fun classProblem(keys: List<MergedKey>, regionOf: (String) -> String): String? {
        val prefixes = keys.map { it.key.substringBefore('.') }.distinct()
        val classNames = keys.map { Naming.classNameFor(it.key) }.distinct()
        val first = keys.first().key
        val problem = when {
            classNames.size > 1 -> Problems.Names.classesDifferInCase(prefixes, classNames)
            classNames.single().lowercase() in reservedTypeNames -> Problems.Names.reservedClass(prefixes.first(), classNames.single())
            prefixes.size > 1 -> Problems.Names.sameClass(prefixes, classNames.single())
            else -> return null
        }
        return messageError(regionOf(first), first, problem)
    }

    private fun methodProblems(className: String, keys: List<MergedKey>, regionOf: (String) -> String): List<String> =
        keys.groupBy { Naming.methodNameFor(it.key) }.flatMap { (method, group) ->
            val first = group.first().key
            listOfNotNull(
                Problems.Names.sameMethod(group.map { it.key }, className, method).takeIf { group.size > 1 },
                Problems.Names.anyMember(method).takeIf { method in reservedMethodNames },
            ).map { messageError(regionOf(first), first, it) }
        }

    private fun enumProblems(className: String, keys: List<MergedKey>, regionOf: (String) -> String): List<String> {
        val enums = keys.flatMap { key ->
            key.placeholders.values.filterIsInstance<SelectPlaceholder>().map { Naming.enumSimpleName(key.key, it.name) to key.key }
        }
        return enums.groupBy { it.first.lowercase() }.values.mapNotNull { sameFile ->
            val owners = sameFile.map { it.second }
            val names = sameFile.map { it.first }.distinct()
            val first = owners.first()
            val problem = when {
                names.size > 1 -> Problems.Names.enumsDifferInCase(owners, className, names)
                owners.size > 1 -> Problems.Names.sameEnum(owners, className, names.single())
                names.single() == className -> Problems.Names.enumNamedLikeClass(className)
                else -> null
            }
            problem?.let { messageError(regionOf(first), first, it) }
        }
    }
}
