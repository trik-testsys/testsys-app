package tech.testsys.infra.database.codegen.jpa

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * KSP service entry point for [CompositeKeyConstructorProcessor].
 *
 * Discovered at runtime via the SPI file at
 * `META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider`.
 *
 * @since %CURRENT_VERSION%
 */
class CompositeKeyConstructorProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        CompositeKeyConstructorProcessor(environment.codeGenerator, environment.logger)
}
