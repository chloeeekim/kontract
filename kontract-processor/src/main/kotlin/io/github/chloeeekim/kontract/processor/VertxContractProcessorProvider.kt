package io.github.chloeeekim.kontract.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class VertxContractProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return VertxContractProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
        )
    }
}
