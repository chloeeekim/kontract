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
    private val serializerMode: SerializerMode = SerializerMode.JACKSON,
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

        val responseType = annotation.arguments
            .firstOrNull { it.name?.asString() == "response" }
            ?.value
            ?.let { it as? com.google.devtools.ksp.symbol.KSType }
            ?.let { type ->
                val qualifiedName = type.declaration.qualifiedName?.asString()
                // KSP2 resolves Nothing::class as java.lang.Void
                if (qualifiedName in setOf("kotlin.Nothing", "java.lang.Void")) null else qualifiedName
            }

        val statusCode = annotation.arguments
            .firstOrNull { it.name?.asString() == "statusCode" }
            ?.value as? Int ?: 200

        if (statusCode !in 100..599) {
            logger.error(
                "Invalid statusCode $statusCode on ${classDecl.simpleName.asString()}. HTTP status codes must be between 100 and 599.",
                classDecl,
            )
        }

        if (statusCode in ContractGenerator.NO_BODY_STATUS_CODES && responseType != null) {
            logger.warn(
                "statusCode $statusCode on ${classDecl.simpleName.asString()} does not allow a response body. " +
                        "The response object will be ignored in routeWithResponse().",
                classDecl,
            )
        }

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
            serializerMode = serializerMode,
            responseType = responseType,
            statusCode = statusCode,
        )

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = false, classDecl.containingFile!!),
            packageName = packageName,
            fileName = contractName,
        )
        file.write(code.toByteArray())
        file.close()

        logger.info("Generated $contractName for $className")

        // If a companion object exists, generate a companion extension function.
        val companionDecl = classDecl.declarations
            .filterIsInstance<KSClassDeclaration>()
            .firstOrNull { it.isCompanionObject }

        if (companionDecl != null) {
            val companionName = companionDecl.simpleName.asString()
            val extensionCode = ContractGenerator.generateCompanionExtensions(
                packageName = packageName,
                className = className,
                responseType = responseType,
                companionName = companionName,
            )

            val extensionFile = codeGenerator.createNewFile(
                dependencies = Dependencies(aggregating = false, classDecl.containingFile!!),
                packageName = packageName,
                fileName = "${className}Extensions",
            )
            extensionFile.write(extensionCode.toByteArray())
            extensionFile.close()

            logger.info("Generated companion extensions for $className")
        }
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

        val converterClass = param.annotations
            .firstOrNull { it.shortName.asString() == "TypeConverter" }
            ?.arguments
            ?.firstOrNull { it.name?.asString() == "converter" }
            ?.value
            ?.let { it as? com.google.devtools.ksp.symbol.KSType }
            ?.declaration?.qualifiedName?.asString()

        if (converterClass != null && source == ParamSource.BODY) {
            logger.error(
                "@TypeConverter on @BodyParam '${param.name?.asString()}' is not supported. " +
                        "Body parameters are deserialized from the request body, not from a string value.",
                param,
            )
        }

        if (converterClass != null && isEnum) {
            logger.warn(
                "@TypeConverter on Enum type '${param.name?.asString()}' overrides built-in Enum parsing.",
                param,
            )
        }

        if (converterClass != null && defaultValue != null) {
            logger.error(
                "@Default on @TypeConverter parameter '${param.name?.asString()}' is not supported. " +
                        "Custom type converters cannot have default values.",
                param,
            )
        }

        if (defaultValue != null && source == ParamSource.BODY) {
            logger.warn(
                "@Default on @BodyParam '${param.name?.asString()}' is ignored. " +
                        "Body parameters are deserialized from the request body and cannot have a default value.",
                param,
            )
        }

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

        val validations = extractValidations(param)

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
            validations = validations,
            converterClass = converterClass,
        )
    }

    private fun extractValidations(param: KSValueParameter): List<ValidationInfo> {
        val resolvedType = param.type.resolve()
        val typeName = resolvedType.declaration.simpleName.asString()
        val baseTypeName = if (resolvedType.isMarkedNullable) typeName else typeName
        val numericTypes = setOf("Int", "Long")
        val stringTypes = setOf("String")
        val paramName = param.name?.asString() ?: "unknown"

        val validations = mutableListOf<ValidationInfo>()
        for (annotation in param.annotations) {
            when (annotation.shortName.asString()) {
                "Min" -> {
                    if (baseTypeName !in numericTypes) {
                        logger.error("@Min is only valid for Int or Long types, but '$paramName' is $typeName.", param)
                    }
                    val value = annotation.arguments.first { it.name?.asString() == "value" }.value as Long
                    validations.add(ValidationInfo.Min(value))
                }
                "Max" -> {
                    if (baseTypeName !in numericTypes) {
                        logger.error("@Max is only valid for Int or Long types, but '$paramName' is $typeName.", param)
                    }
                    val value = annotation.arguments.first { it.name?.asString() == "value" }.value as Long
                    validations.add(ValidationInfo.Max(value))
                }
                "NotBlank" -> {
                    if (baseTypeName !in stringTypes) {
                        logger.error("@NotBlank is only valid for String types, but '$paramName' is $typeName.", param)
                    }
                    validations.add(ValidationInfo.NotBlank)
                }
                "Size" -> {
                    if (baseTypeName !in numericTypes + stringTypes) {
                        logger.error("@Size is only valid for Int, Long, or String types, but '$paramName' is $typeName.", param)
                    }
                    val min = annotation.arguments.first { it.name?.asString() == "min" }.value as Int
                    val max = annotation.arguments.first { it.name?.asString() == "max" }.value as Int
                    validations.add(ValidationInfo.Size(min, max))
                }
                "Pattern" -> {
                    if (baseTypeName !in stringTypes) {
                        logger.error("@Pattern is only valid for String types, but '$paramName' is $typeName.", param)
                    }
                    val regex = annotation.arguments.first { it.name?.asString() == "regex" }.value as String
                    validations.add(ValidationInfo.Pattern(regex))
                }
            }
        }
        return validations
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
