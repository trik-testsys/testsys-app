package tech.testsys.infra.codegen.jpa

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * KSP service entry point for [FieldNamesProcessor].
 *
 * Discovered at runtime via the SPI file at
 * `META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider`.
 *
 * @since %CURRENT_VERSION%
 */
class FieldNamesProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        FieldNamesProcessor(environment.codeGenerator, environment.logger)
}
