package io.github.chloeeekim.kontract.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.validate

class VertxContractProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(VERTX_ENDPOINT_ANNOTATION)
            .filterIsInstance<KSClassDeclaration>()
            .toList()

        val unprocessed = symbols.filter { !it.validate() }

        symbols.filter { it.validate() }.forEach { classDecl ->
            generateContract(classDecl)
        }

        return unprocessed
    }

    private fun generateContract(classDecl: KSClassDeclaration) {
        val annotation = classDecl.annotations
            .first { it.shortName.asString() == "VertxEndpoint" }

        val method = annotation.arguments
            .first { it.name?.asString() == "method" }
            .value.toString()
            .substringAfterLast(".")
        val path = annotation.arguments
            .first { it.name?.asString() == "path" }
            .value.toString()

        val params = classDecl.primaryConstructor?.parameters
            ?.map { extractParamInfo(it) }
            ?: emptyList()

        val bodyParams = params.filter { it.source == ParamSource.BODY }
        if (bodyParams.size > 1) {
            logger.error(
                "Only one @BodyParam is allowed per endpoint, but ${classDecl.simpleName.asString()} has ${bodyParams.size}: " +
                        bodyParams.joinToString { it.name },
                classDecl,
            )
        }

        val packageName = classDecl.packageName.asString()
        val className = classDecl.simpleName.asString()
        val contractName = "${className}Contract"

        val code = ContractGenerator.generate(
            packageName = packageName,
            className = className,
            httpMethod = method,
            path = path,
            params = params,
        )

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = false, classDecl.containingFile!!),
            packageName = packageName,
            fileName = contractName,
        )
        file.write(code.toByteArray())
        file.close()

        logger.info("Generated $contractName for $className")
    }

    private fun extractParamInfo(param: KSValueParameter): ParamInfo {
        val paramAnnotation = param.annotations
            .firstOrNull { it.shortName.asString() in PARAM_ANNOTATIONS }
            ?: error("Parameter '${param.name?.asString()}' must have a source annotation (@PathParam, @QueryParam, @HeaderParam, @CookieParam, or @BodyParam)")

        val annotationName = if (paramAnnotation.shortName.asString() == "BodyParam") {
            ""
        } else {
            paramAnnotation.arguments
                .firstOrNull { it.name?.asString() == "name" }
                ?.value?.toString() ?: ""
        }

        val resolvedType = param.type.resolve()
        val typeDecl = resolvedType.declaration
        val typeName = typeDecl.simpleName.asString()
        val qualifiedTypeName = typeDecl.qualifiedName?.asString() ?: typeName
        val nullable = resolvedType.isMarkedNullable

        val isEnum = typeDecl is KSClassDeclaration && typeDecl.classKind == ClassKind.ENUM_CLASS

        val source = when (paramAnnotation.shortName.asString()) {
            "PathParam" -> ParamSource.PATH
            "QueryParam" -> ParamSource.QUERY
            "HeaderParam" -> ParamSource.HEADER
            "CookieParam" -> ParamSource.COOKIE
            "BodyParam" -> ParamSource.BODY
            else -> error("Unknown param annotation: ${paramAnnotation.shortName.asString()}")
        }

        val defaultValue = param.annotations
            .firstOrNull { it.shortName.asString() == "Default" }
            ?.arguments
            ?.firstOrNull { it.name?.asString() == "value" }
            ?.value?.toString()

        val enumIgnoreCase = param.annotations
            .any { it.shortName.asString() == "EnumIgnoreCase" }

        if (defaultValue != null && source == ParamSource.PATH) {
            logger.warn(
                "@Default on @PathParam '${param.name?.asString()}' is discouraged. " +
                        "Path parameters are part of the URL and should always be present. " +
                        "A default value may hide routing errors.",
                param,
            )
        }

        if (enumIgnoreCase && !isEnum) {
            logger.error(
                "@EnumIgnoreCase on '${param.name?.asString()}' is only valid for Enum types, " +
                        "but the type is '$typeName'.",
                param,
            )
        }

        validateDefaultValue(param, typeName, defaultValue, isEnum, typeDecl)

        return ParamInfo(
            name = param.name!!.asString(),
            typeName = typeName,
            qualifiedTypeName = qualifiedTypeName,
            source = source,
            nullable = nullable,
            annotationName = annotationName,
            defaultValue = defaultValue,
            isEnum = isEnum,
            enumIgnoreCase = enumIgnoreCase,
        )
    }

    private fun validateDefaultValue(
        param: KSValueParameter,
        typeName: String,
        defaultValue: String?,
        isEnum: Boolean,
        typeDecl: com.google.devtools.ksp.symbol.KSDeclaration,
    ) {
        if (defaultValue == null) return

        when {
            typeName == "Int" && defaultValue.toIntOrNull() == null -> {
                logger.error(
                    "@Default value '$defaultValue' is not a valid Int for '${param.name?.asString()}'.",
                    param,
                )
            }
            typeName == "Long" && defaultValue.toLongOrNull() == null -> {
                logger.error(
                    "@Default value '$defaultValue' is not a valid Long for '${param.name?.asString()}'.",
                    param,
                )
            }
            isEnum -> {
                val enumDecl = typeDecl as KSClassDeclaration
                val entries = enumDecl.declarations
                    .filterIsInstance<KSClassDeclaration>()
                    .filter { it.classKind == ClassKind.ENUM_ENTRY }
                    .map { it.simpleName.asString() }
                    .toList()
                // Extract the "ASC" part from @Default("SortOrder.ASC")
                val enumValue = defaultValue.substringAfterLast(".")
                if (enumValue !in entries) {
                    logger.error(
                        "@Default value '$defaultValue' is not a valid ${typeName} entry for '${param.name?.asString()}'. " +
                                "Allowed: ${entries.joinToString()}",
                        param,
                    )
                }
            }
        }
    }

    companion object {
        private const val VERTX_ENDPOINT_ANNOTATION = "io.github.chloeeekim.kontract.annotation.VertxEndpoint"
        private val PARAM_ANNOTATIONS = setOf("PathParam", "QueryParam", "HeaderParam", "CookieParam", "BodyParam")
    }
}
