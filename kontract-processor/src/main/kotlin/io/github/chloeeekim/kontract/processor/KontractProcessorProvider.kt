package io.github.chloeeekim.kontract.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class KontractProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val serializerOption = environment.options["kontract.serializer"] ?: "jackson"
        val serializerMode = when (serializerOption.lowercase()) {
            "jackson" -> SerializerMode.JACKSON
            "kotlinx" -> SerializerMode.KOTLINX
            else -> {
                environment.logger.warn("Unknown serializer '$serializerOption', falling back to jackson")
                SerializerMode.JACKSON
            }
        }

        return KontractProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
            serializerMode = serializerMode,
        )
    }
}
