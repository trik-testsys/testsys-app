package tech.testsys.infra.database.codegen.jpa

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * KSP service entry point of [FieldNamesProcessor], registered in `META-INF/services`.
 *
 * @since %CURRENT_VERSION%
 */
class FieldNamesProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        FieldNamesProcessor(environment.codeGenerator, environment.logger)
}
