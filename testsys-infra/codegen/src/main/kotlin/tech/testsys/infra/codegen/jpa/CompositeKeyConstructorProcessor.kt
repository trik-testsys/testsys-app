package tech.testsys.infra.codegen.jpa

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

/**
 * KSP processor that emits a top-level "fake constructor" function for every class
 * annotated with [CompositeKeyConstructor]. See spec for the contract.
 *
 * @since %CURRENT_VERSION%
 */
internal class CompositeKeyConstructorProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getSymbolsWithAnnotation(COMPOSITE_KEY_CONSTRUCTOR_FQN)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { processEntity(it) }
        return emptyList()
    }

    private fun processEntity(entity: KSClassDeclaration) {
        val idType = resolveCompositeIdType(entity) ?: return
        if (!entityHasSingleIdParam(entity)) return
        if (!isDataClass(idType, entity)) return
        val idCtor = idType.primaryConstructor ?: run {
            logger.error(
                "@CompositeKeyConstructor: composite id ${idType.simpleName.asString()} has no primary constructor",
                entity,
            )
            return
        }
        if (!allParamsAreVal(idCtor, idType, entity)) return
        emitFactoryFunction(entity, idType, idCtor)
    }

    private fun allParamsAreVal(idCtor: KSFunctionDeclaration, idType: KSClassDeclaration, reportNode: KSClassDeclaration): Boolean {
        val violations = idCtor.parameters.filter { !it.isVal }
        violations.forEach { p ->
            logger.error(
                "@CompositeKeyConstructor: id ${idType.simpleName.asString()} parameter " +
                    "'${p.name?.asString()}' must be declared `val`",
                reportNode,
            )
        }
        return violations.isEmpty()
    }

    private fun isDataClass(idType: KSClassDeclaration, reportNode: KSClassDeclaration): Boolean {
        val isData = Modifier.DATA in idType.modifiers
        if (!isData) {
            logger.error(
                "@CompositeKeyConstructor: composite id ${idType.simpleName.asString()} must be a Kotlin `data class`",
                reportNode,
            )
        }
        return isData
    }

    private fun entityHasSingleIdParam(entity: KSClassDeclaration): Boolean {
        val params = entity.primaryConstructor?.parameters.orEmpty()
        val hasSingleIdParam = params.size == 1 && params[0].name?.asString() == "id"
        if (!hasSingleIdParam) {
            logger.error(
                "@CompositeKeyConstructor: ${entity.simpleName.asString()} must have a single primary constructor parameter named 'id'",
                entity,
            )
        }
        return hasSingleIdParam
    }

    private fun resolveCompositeIdType(entity: KSClassDeclaration): KSClassDeclaration? {
        val supertype = entity.superTypes
            .map { it.resolve() }
            .firstOrNull {
                it.declaration.qualifiedName?.asString() == COMPOSITE_JPA_ENTITY_FQN
            }
        if (supertype == null) {
            logger.error(
                "@CompositeKeyConstructor: class ${entity.simpleName.asString()} must extend CompositeJpaEntity<T>",
                entity,
            )
            return null
        }
        return supertype.arguments.firstOrNull()
            ?.type?.resolve()?.declaration as? KSClassDeclaration
    }

    private fun emitFactoryFunction(entity: KSClassDeclaration, idType: KSClassDeclaration, idCtor: KSFunctionDeclaration) {
        val entityClassName: ClassName = entity.toClassName()
        val idClassName: ClassName = idType.toClassName()

        val params = idCtor.parameters.map { p ->
            ParameterSpec.builder(
                name = p.name?.asString() ?: error("Anonymous parameter in $idClassName"),
                type = p.type.toTypeName(),
            ).build()
        }

        val idCtorCall = CodeBlock.builder()
            .add("%T(\n", idClassName)
            .indent()
            .apply {
                params.forEach { p -> add("%N = %N,\n", p, p) }
            }
            .unindent()
            .add(")")
            .build()

        val body = CodeBlock.builder()
            .add("return %T(\n", entityClassName)
            .indent()
            .add("%L,\n", idCtorCall)
            .unindent()
            .add(")\n")
            .build()

        val function = FunSpec.builder(entity.simpleName.asString())
            .addParameters(params)
            .returns(entityClassName)
            .addCode(body)
            .build()

        val file = FileSpec.builder(
            packageName = entity.packageName.asString(),
            fileName = "${entity.simpleName.asString()}\$Constructors",
        )
            .indent("    ")
            .addFileComment("Generated by CompositeKeyConstructorProcessor. Do not edit.")
            .addFunction(function)
            .build()

        // kotlinpoet-ksp's `writeTo` constructs the right KSP `Dependencies` object
        // from the originating files; no need to build it manually.
        file.writeTo(
            codeGenerator = codeGenerator,
            aggregating = false,
            originatingKSFiles = listOfNotNull(entity.containingFile),
        )
    }

    private companion object {

        const val COMPOSITE_JPA_ENTITY_FQN = "tech.testsys.infra.database.jpa.entity.CompositeJpaEntity"
        const val COMPOSITE_KEY_CONSTRUCTOR_FQN = "tech.testsys.infra.codegen.jpa.CompositeKeyConstructor"
    }
}
