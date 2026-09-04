package tech.testsys.infra.database.codegen.jpa

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * KSP service entry point of [CompositeKeyConstructorProcessor], registered in `META-INF/services`.
 *
 * @since %CURRENT_VERSION%
 */
class CompositeKeyConstructorProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        CompositeKeyConstructorProcessor(environment.codeGenerator, environment.logger)
}
