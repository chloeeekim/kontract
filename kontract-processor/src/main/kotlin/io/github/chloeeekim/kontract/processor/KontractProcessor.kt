package io.github.chloeeekim.kontract.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate

class KontractProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val serializerMode: SerializerMode = SerializerMode.JACKSON,
    private val coroutines: Boolean = false,
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

        val method = annotation.requireArg("method").toString().substringAfterLast(".")
        val path = annotation.requireArg("path").toString()

        val responseKSType = (annotation.optionalArg("response") as? KSType)
            ?.takeIf {
                val qualifiedName = it.declaration.qualifiedName?.asString()
                qualifiedName !in setOf("kotlin.Nothing", "java.lang.Void")
            }
        val responseType = responseKSType?.declaration?.qualifiedName?.asString()

        if (serializerMode == SerializerMode.KOTLINX && responseKSType != null) {
            val responseDecl = responseKSType.declaration as? KSClassDeclaration
            if (responseDecl?.classKind == ClassKind.INTERFACE) {
                logger.warn(
                    "Response type '${responseDecl.simpleName.asString()}' is an interface. " +
                            "kotlinx.serialization requires concrete @Serializable types.",
                    classDecl,
                )
            } else if (responseDecl != null && Modifier.ABSTRACT in responseDecl.modifiers) {
                logger.warn(
                    "Response type '${responseDecl.simpleName.asString()}' is abstract. " +
                            "kotlinx.serialization requires concrete @Serializable types.",
                    classDecl,
                )
            }
        }

        val statusCode = annotation.optionalArg("statusCode") as? Int ?: 200

        if (statusCode !in 100..599) {
            logger.error(
                "Invalid statusCode $statusCode on ${classDecl.simpleName.asString()}. HTTP status codes must be between 100 and 599.",
                classDecl,
            )
        }

        if (statusCode in KontractCodeGenerator.NO_BODY_STATUS_CODES && responseType != null) {
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

        val code = KontractCodeGenerator.generate(
            packageName = packageName,
            className = className,
            httpMethod = method,
            path = path,
            params = params,
            serializerMode = serializerMode,
            responseType = responseType,
            statusCode = statusCode,
            coroutines = coroutines,
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
            val extensionCode = KontractCodeGenerator.generateCompanionExtensions(
                packageName = packageName,
                className = className,
                responseType = responseType,
                companionName = companionName,
                coroutines = coroutines,
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
            paramAnnotation.optionalArg("name")?.toString() ?: ""
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
            ?.optionalArg("value")?.toString()

        val enumIgnoreCase = param.annotations
            .any { it.shortName.asString() == "EnumIgnoreCase" }

        val converterClass = param.annotations
            .firstOrNull { it.shortName.asString() == "TypeConverter" }
            ?.let { (it.optionalArg("converter") as? KSType) }
            ?.declaration?.qualifiedName?.asString()

        val validations = extractValidations(param)

        val paramInfo = ParamInfo(
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

        validateParamAnnotations(param, paramInfo, typeDecl)
        return paramInfo
    }

    private fun validateParamAnnotations(
        param: KSValueParameter,
        info: ParamInfo,
        typeDecl: KSDeclaration,
    ) {
        val paramName = param.name?.asString()

        if (info.converterClass != null && info.source == ParamSource.BODY) {
            logger.error(
                "@TypeConverter on @BodyParam '$paramName' is not supported. " +
                        "Body parameters are deserialized from the request body, not from a string value.",
                param,
            )
        }

        if (info.converterClass != null && info.isEnum) {
            logger.warn(
                "@TypeConverter on Enum type '$paramName' overrides built-in Enum parsing.",
                param,
            )
        }

        if (info.converterClass != null && info.defaultValue != null) {
            logger.error(
                "@Default on @TypeConverter parameter '$paramName' is not supported. " +
                        "Custom type converters cannot have default values.",
                param,
            )
        }

        if (info.defaultValue != null && info.source == ParamSource.BODY) {
            logger.warn(
                "@Default on @BodyParam '$paramName' is ignored. " +
                        "Body parameters are deserialized from the request body and cannot have a default value.",
                param,
            )
        }

        if (info.defaultValue != null && info.source == ParamSource.PATH) {
            logger.warn(
                "@Default on @PathParam '$paramName' is discouraged. " +
                        "Path parameters are part of the URL and should always be present. " +
                        "A default value may hide routing errors.",
                param,
            )
        }

        if (info.enumIgnoreCase && !info.isEnum) {
            logger.error(
                "@EnumIgnoreCase on '$paramName' is only valid for Enum types, " +
                        "but the type is '${info.typeName}'.",
                param,
            )
        }

        validateDefaultValue(param, info.typeName, info.defaultValue, info.isEnum, typeDecl)
    }

    private fun extractValidations(param: KSValueParameter): List<ValidationInfo> {
        val typeName = param.type.resolve().declaration.simpleName.asString()
        val numericTypes = setOf("Int", "Long")
        val stringTypes = setOf("String")
        val paramName = param.name?.asString() ?: "unknown"

        val validations = mutableListOf<ValidationInfo>()
        for (annotation in param.annotations) {
            when (annotation.shortName.asString()) {
                "Min" -> {
                    if (typeName !in numericTypes) {
                        logger.error("@Min is only valid for Int or Long types, but '$paramName' is $typeName.", param)
                    }
                    validations.add(ValidationInfo.Min(annotation.requireArg("value") as Long))
                }
                "Max" -> {
                    if (typeName !in numericTypes) {
                        logger.error("@Max is only valid for Int or Long types, but '$paramName' is $typeName.", param)
                    }
                    validations.add(ValidationInfo.Max(annotation.requireArg("value") as Long))
                }
                "NotBlank" -> {
                    if (typeName !in stringTypes) {
                        logger.error("@NotBlank is only valid for String types, but '$paramName' is $typeName.", param)
                    }
                    validations.add(ValidationInfo.NotBlank)
                }
                "Size" -> {
                    if (typeName !in numericTypes + stringTypes) {
                        logger.error("@Size is only valid for Int, Long, or String types, but '$paramName' is $typeName.", param)
                    }
                    validations.add(ValidationInfo.Size(
                        min = annotation.requireArg("min") as Int,
                        max = annotation.requireArg("max") as Int,
                    ))
                }
                "Pattern" -> {
                    if (typeName !in stringTypes) {
                        logger.error("@Pattern is only valid for String types, but '$paramName' is $typeName.", param)
                    }
                    validations.add(ValidationInfo.Pattern(annotation.requireArg("regex") as String))
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
        typeDecl: KSDeclaration,
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
                        "@Default value '$defaultValue' is not a valid $typeName entry for '${param.name?.asString()}'. " +
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

        private fun KSAnnotation.requireArg(name: String): Any =
            arguments.first { it.name?.asString() == name }.value!!

        private fun KSAnnotation.optionalArg(name: String): Any? =
            arguments.firstOrNull { it.name?.asString() == name }?.value
    }
}
