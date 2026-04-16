package io.github.chloeeekim.kontract.processor

/**
 * Generates the Kotlin source code for the Contract object.
 */
object KontractCodeGenerator {

    fun generateCompanionExtensions(
        packageName: String,
        className: String,
        responseType: String? = null,
        companionName: String = "Companion",
        coroutines: Boolean = false,
    ): String {
        val contractName = "${className}Contract"
        val responseSimpleName = responseType?.substringAfterLast(".")

        return buildString {
            if (packageName.isNotEmpty()) {
                appendLine("package $packageName")
                appendLine()
            }
            appendLine("import io.vertx.ext.web.Router")
            appendLine("import io.vertx.ext.web.RoutingContext")
            if (coroutines) {
                appendLine("import kotlinx.coroutines.CoroutineScope")
            }
            if (responseType != null && responseType.substringBeforeLast(".") != packageName) {
                appendLine("import $responseType")
            }
            appendLine()
            appendLine("fun $className.$companionName.from(ctx: RoutingContext) =")
            appendLine("    $contractName.from(ctx)")
            appendLine()
            appendLine("fun $className.$companionName.route(router: Router, handler: ($className, RoutingContext) -> Unit) =")
            appendLine("    $contractName.route(router, handler)")
            if (responseSimpleName != null) {
                appendLine()
                appendLine("fun $className.$companionName.routeWithResponse(router: Router, handler: ($className, RoutingContext) -> $responseSimpleName) =")
                appendLine("    $contractName.routeWithResponse(router, handler)")
            }
            if (coroutines) {
                appendLine()
                appendLine("fun $className.$companionName.coRoute(scope: CoroutineScope, router: Router, handler: suspend ($className, RoutingContext) -> Unit) =")
                appendLine("    $contractName.coRoute(scope, router, handler)")
                if (responseSimpleName != null) {
                    appendLine()
                    appendLine("fun $className.$companionName.coRouteWithResponse(scope: CoroutineScope, router: Router, handler: suspend ($className, RoutingContext) -> $responseSimpleName) =")
                    appendLine("    $contractName.coRouteWithResponse(scope, router, handler)")
                }
            }
        }
    }

    fun generate(
        packageName: String,
        className: String,
        httpMethod: String,
        path: String,
        params: List<ParamInfo>,
        serializerMode: SerializerMode = SerializerMode.JACKSON,
        responseType: String? = null,
        statusCode: Int = 200,
        coroutines: Boolean = false,
    ): String {
        val contractName = "${className}Contract"
        val imports = collectImports(params, serializerMode, responseType, packageName, coroutines)
        val responseSimpleName = responseType?.substringAfterLast(".")

        return buildString {
            appendLine(generatePackageAndImports(packageName, imports))
            appendLine("object $contractName {")
            append(generateFields(params, serializerMode, responseType))
            appendLine()
            appendLine(generateFromMethod(className, params, serializerMode))
            appendLine()
            appendLine(generateRouteMethods(className, responseSimpleName, httpMethod, path, statusCode, serializerMode, coroutines))
            appendLine("}")
        }
    }

    private fun generatePackageAndImports(packageName: String, imports: Set<String>): String = buildString {
        if (packageName.isNotEmpty()) {
            appendLine("package $packageName")
            appendLine()
        }
        imports.sorted().forEach { appendLine("import $it") }
    }

    private fun generateFields(params: List<ParamInfo>, serializerMode: SerializerMode, responseType: String?): String {
        val needsObjectMapper = serializerMode == SerializerMode.JACKSON &&
                (params.any { it.source == ParamSource.BODY } || responseType != null)

        return buildString {
            if (needsObjectMapper) {
                appendLine()
                appendLine("    private val objectMapper = jacksonObjectMapper()")
            }
            for ((fieldName, simpleName) in collectConverterFields(params)) {
                appendLine("    private val $fieldName = $simpleName()")
            }
            for ((fieldName, regex) in collectRegexFields(params)) {
                appendLine("    private val $fieldName = Regex(\"${escapeStringLiteral(regex)}\")")
            }
        }
    }

    private fun generateFromMethod(className: String, params: List<ParamInfo>, serializerMode: SerializerMode): String {
        val parsingLines = params.joinToString("\n\n") { generateParamExtraction(it, serializerMode) }
        val constructorArgs = params.joinToString("\n") { "            ${it.name} = ${it.name}," }

        return buildString {
            appendLine("    fun from(ctx: RoutingContext): $className {")
            appendLine(parsingLines.prependIndent("        "))
            appendLine()
            appendLine("        return $className(")
            appendLine(constructorArgs)
            appendLine("        )")
            append("    }")
        }
    }

    private fun generateRouteMethods(
        className: String,
        responseSimpleName: String?,
        httpMethod: String,
        path: String,
        statusCode: Int,
        serializerMode: SerializerMode,
        coroutines: Boolean,
    ): String = buildString {
        append(generateRouteMethodInternal(className, httpMethod, path, isSuspend = false))
        if (responseSimpleName != null) {
            appendLine()
            appendLine()
            append(generateTypedRouteMethodInternal(className, responseSimpleName, httpMethod, path, statusCode, serializerMode, isSuspend = false))
        }
        if (coroutines) {
            appendLine()
            appendLine()
            append(generateRouteMethodInternal(className, httpMethod, path, isSuspend = true))
            if (responseSimpleName != null) {
                appendLine()
                appendLine()
                append(generateTypedRouteMethodInternal(className, responseSimpleName, httpMethod, path, statusCode, serializerMode, isSuspend = true))
            }
        }
    }

    private fun collectImports(
        params: List<ParamInfo>,
        serializerMode: SerializerMode,
        responseType: String? = null,
        packageName: String = "",
        coroutines: Boolean = false,
    ): Set<String> {
        val imports = mutableSetOf(
            "io.github.chloeeekim.kontract.annotation.BadRequestException",
            "io.github.chloeeekim.kontract.annotation.KontractConfig",
            "io.vertx.ext.web.Router",
            "io.vertx.ext.web.RoutingContext",
        )
        for (param in params) {
            if (param.isEnum) {
                imports.addIfDifferentPackage(param.qualifiedTypeName, packageName)
            }
            if (param.converterClass != null) {
                imports.addIfDifferentPackage(param.converterClass, packageName)
                imports.addIfDifferentPackage(param.qualifiedTypeName, packageName)
            }
            if (param.source == ParamSource.BODY) {
                imports.addIfDifferentPackage(param.qualifiedTypeName, packageName)
                when (serializerMode) {
                    SerializerMode.JACKSON -> imports.add("com.fasterxml.jackson.module.kotlin.jacksonObjectMapper")
                    SerializerMode.KOTLINX -> imports.add("kotlinx.serialization.json.Json")
                }
            }
        }
        if (responseType != null) {
            imports.addIfDifferentPackage(responseType, packageName)
            when (serializerMode) {
                SerializerMode.JACKSON -> imports.add("com.fasterxml.jackson.module.kotlin.jacksonObjectMapper")
                SerializerMode.KOTLINX -> imports.add("kotlinx.serialization.json.Json")
            }
        }
        if (coroutines) {
            imports.add("kotlinx.coroutines.CoroutineScope")
            imports.add("kotlinx.coroutines.launch")
        }
        return imports
    }

    private fun MutableSet<String>.addIfDifferentPackage(qualifiedName: String, packageName: String) {
        if (qualifiedName.substringBeforeLast(".") != packageName) {
            add(qualifiedName)
        }
    }

    private fun collectRegexFields(params: List<ParamInfo>): List<Pair<String, String>> {
        return params.flatMap { param ->
            param.validations.filterIsInstance<ValidationInfo.Pattern>().map { pattern ->
                "${param.name}Pattern" to pattern.regex
            }
        }
    }

    private fun converterFieldName(converterClass: String): String {
        val simpleName = converterClass.substringAfterLast(".")
        return simpleName.replaceFirstChar { it.lowercase() }
    }

    private fun collectConverterFields(params: List<ParamInfo>): List<Pair<String, String>> {
        return params
            .filter { it.converterClass != null }
            .map { it.converterClass!! }
            .distinct()
            .map { converterClass ->
                val simpleName = converterClass.substringAfterLast(".")
                converterFieldName(converterClass) to simpleName
            }
    }

    internal val NO_BODY_STATUS_CODES = setOf(204, 205, 304)

    // isSuspend=true: coRoute (coroutine based asynchronous handler)
    // isSuspend=false: route (synchronous handler)
    private fun generateRouteMethodInternal(className: String, httpMethod: String, path: String, isSuspend: Boolean): String {
        val routerMethod = httpMethod.lowercase()
        val methodName = if (isSuspend) "coRoute" else "route"
        val suspendKeyword = if (isSuspend) "suspend " else ""
        val scopeParam = if (isSuspend) "scope: CoroutineScope, " else ""
        val catchAll = if (isSuspend) """
                } catch (e: Exception) {
                    ctx.fail(e)""" else ""

        return if (isSuspend) {
            """    fun $methodName(${scopeParam}router: Router, handler: ${suspendKeyword}($className, RoutingContext) -> Unit) {
        router.$routerMethod("$path").handler { ctx ->
            scope.launch {
                try {
                    val request = from(ctx)
                    handler(request, ctx)
                } catch (e: BadRequestException) {
                    KontractConfig.errorHandler.handleError(ctx, e)$catchAll
                }
            }
        }
    }"""
        } else {
            """    fun $methodName(${scopeParam}router: Router, handler: ${suspendKeyword}($className, RoutingContext) -> Unit) {
        router.$routerMethod("$path").handler { ctx ->
            try {
                val request = from(ctx)
                handler(request, ctx)
            } catch (e: BadRequestException) {
                KontractConfig.errorHandler.handleError(ctx, e)
            }
        }
    }"""
        }
    }

    private fun generateTypedRouteMethodInternal(
        className: String,
        responseSimpleName: String,
        httpMethod: String,
        path: String,
        statusCode: Int,
        serializerMode: SerializerMode,
        isSuspend: Boolean,
    ): String {
        val routerMethod = httpMethod.lowercase()
        val methodName = if (isSuspend) "coRouteWithResponse" else "routeWithResponse"
        val suspendKeyword = if (isSuspend) "suspend " else ""
        val scopeParam = if (isSuspend) "scope: CoroutineScope, " else ""
        val catchAll = if (isSuspend) """
                } catch (e: Exception) {
                    ctx.fail(e)""" else ""

        val handlerBody = if (statusCode in NO_BODY_STATUS_CODES) {
            """val request = from(ctx)
                    handler(request, ctx)
                    ctx.response().setStatusCode($statusCode).end()"""
        } else {
            val serializeExpr = when (serializerMode) {
                SerializerMode.JACKSON -> "objectMapper.writeValueAsString(response)"
                SerializerMode.KOTLINX -> "Json.encodeToString(response)"
            }
            """val request = from(ctx)
                    val response = handler(request, ctx)
                    ctx.response()
                        .setStatusCode($statusCode)
                        .putHeader("Content-Type", "application/json")
                        .end($serializeExpr)"""
        }

        return if (isSuspend) {
            """    fun $methodName(${scopeParam}router: Router, handler: ${suspendKeyword}($className, RoutingContext) -> $responseSimpleName) {
        router.$routerMethod("$path").handler { ctx ->
            scope.launch {
                try {
                    $handlerBody
                } catch (e: BadRequestException) {
                    KontractConfig.errorHandler.handleError(ctx, e)$catchAll
                }
            }
        }
    }"""
        } else {
            """    fun $methodName(${scopeParam}router: Router, handler: ${suspendKeyword}($className, RoutingContext) -> $responseSimpleName) {
        router.$routerMethod("$path").handler { ctx ->
            try {
                $handlerBody
            } catch (e: BadRequestException) {
                KontractConfig.errorHandler.handleError(ctx, e)
            }
        }
    }"""
        }
    }

    private fun generateParamExtraction(param: ParamInfo, serializerMode: SerializerMode): String {
        val paramName = param.annotationName.ifEmpty { param.name }

        val parsing = when (param.source) {
            ParamSource.PATH -> generatePathParamParsing(param, paramName)
            ParamSource.QUERY -> generateQueryParamParsing(param, paramName)
            ParamSource.HEADER -> generateHeaderParamParsing(param, paramName)
            ParamSource.COOKIE -> generateCookieParamParsing(param, paramName)
            ParamSource.BODY -> generateBodyParamParsing(param, serializerMode)
        }

        val validationLines = generateValidation(param)
        return if (validationLines.isEmpty()) parsing else "$parsing\n$validationLines"
    }

    private fun escapeStringLiteral(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\$", "\\\$")
    }

    private fun generateMissingFallback(param: ParamInfo, paramName: String, paramKind: String): String {
        return when {
            param.defaultValue != null && param.typeName == "String" ->
                """?: "${escapeStringLiteral(param.defaultValue)}""""
            param.defaultValue != null && param.isEnum ->
                "?: ${resolveEnumDefault(param)}"
            param.defaultValue != null -> "?: ${param.defaultValue}"
            param.nullable -> ""
            else -> """?: throw BadRequestException("Missing $paramKind param: $paramName")"""
        }
    }

    private fun resolveEnumDefault(param: ParamInfo): String {
        val entryName = param.defaultValue!!.substringAfterLast(".")
        return "${param.qualifiedTypeName}.$entryName"
    }

    // --- Path Param ---

    private fun generatePathParamParsing(param: ParamInfo, paramName: String): String {
        val extractor = """ctx.pathParam("$paramName")"""
        return generateTypedParsing(param, paramName, extractor, "path")
    }

    // --- Query Param ---

    private fun generateQueryParamParsing(param: ParamInfo, paramName: String): String {
        val extractor = """ctx.request().getParam("$paramName")"""
        return generateTypedParsing(param, paramName, extractor, "query")
    }

    // --- Header Param ---

    private fun generateHeaderParamParsing(param: ParamInfo, paramName: String): String {
        val extractor = """ctx.request().getHeader("$paramName")"""
        return generateTypedParsing(param, paramName, extractor, "header")
    }

    // --- Cookie Param ---

    private fun generateCookieParamParsing(param: ParamInfo, paramName: String): String {
        val extractor = """ctx.request().getCookie("$paramName")?.value"""
        return generateTypedParsing(param, paramName, extractor, "cookie")
    }

    // --- Common typed parsing (shared by Path, Query, Header, Cookie) ---

    private fun generateTypedParsing(param: ParamInfo, paramName: String, extractor: String, paramKind: String): String {
        if (param.converterClass != null) {
            return generateConverterParsing(param, paramName, extractor, paramKind)
        }

        if (param.isEnum) {
            return generateEnumParsing(param, paramName, extractor, paramKind)
        }

        return when (param.typeName) {
            "Long", "Int" -> generateNumericParsing(param, paramName, extractor, paramKind)
            "String" -> generateStringParsing(param, paramName, extractor, paramKind)
            else -> error("Unsupported $paramKind param type: ${param.typeName}")
        }
    }

    // --- Body Param ---

    private fun generateBodyParamParsing(param: ParamInfo, serializerMode: SerializerMode): String {
        val typeName = param.typeName
        val deserializeExpr = when (serializerMode) {
            SerializerMode.JACKSON -> "objectMapper.readValue(ctx.body().buffer().bytes, $typeName::class.java)"
            SerializerMode.KOTLINX -> "Json.decodeFromString<$typeName>(ctx.body().asString())"
        }
        val comment = if (serializerMode == SerializerMode.KOTLINX) {
            "// $typeName must be annotated with @kotlinx.serialization.Serializable\n"
        } else {
            ""
        }
        return """${comment}val ${param.name} = try {
    $deserializeExpr
} catch (e: Exception) {
    throw BadRequestException("Invalid request body: ${'$'}{e.message}")
}"""
    }

    // --- Numeric (Int/Long) with ?.let pattern ---

    private fun generateNumericParsing(param: ParamInfo, paramName: String, extractor: String, paramKind: String): String {
        val converter = if (param.typeName == "Long") "toLongOrNull" else "toIntOrNull"
        val fallback = generateMissingFallback(param, paramName, paramKind)

        return if (fallback.isEmpty()) {
            """val ${param.name} = $extractor?.let { raw ->
    raw.$converter()
        ?: throw BadRequestException("Invalid value for $paramKind param '$paramName': '${'$'}raw'")
}"""
        } else {
            """val ${param.name} = $extractor?.let { raw ->
    raw.$converter()
        ?: throw BadRequestException("Invalid value for $paramKind param '$paramName': '${'$'}raw'")
}
    $fallback""".trimEnd()
        }
    }

    // --- String ---

    private fun generateStringParsing(param: ParamInfo, paramName: String, extractor: String, paramKind: String): String {
        val fallback = generateMissingFallback(param, paramName, paramKind)
        return if (fallback.isEmpty()) {
            """val ${param.name} = $extractor"""
        } else {
            """val ${param.name} = $extractor
    $fallback""".trimEnd()
        }
    }

    // --- Enum ---

    private fun generateEnumParsing(param: ParamInfo, paramName: String, extractor: String, paramKind: String): String {
        val enumType = param.qualifiedTypeName
        val parser = if (param.enumIgnoreCase) {
            """$enumType.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
            ?: throw BadRequestException("Invalid value for $paramKind param '$paramName': '${'$'}raw'. Allowed: ${'$'}{$enumType.entries.joinToString()}")"""
        } else {
            """try { $enumType.valueOf(raw) }
        catch (e: IllegalArgumentException) {
            throw BadRequestException("Invalid value for $paramKind param '$paramName': '${'$'}raw'. Allowed: ${'$'}{$enumType.entries.joinToString()}")
        }"""
        }

        val fallback = generateMissingFallback(param, paramName, paramKind)

        return if (fallback.isEmpty()) {
            """val ${param.name} = $extractor?.let { raw ->
    $parser
}"""
        } else {
            """val ${param.name} = $extractor?.let { raw ->
    $parser
}
    $fallback""".trimEnd()
        }
    }

    // --- Custom TypeConverter ---

    private fun generateConverterParsing(param: ParamInfo, paramName: String, extractor: String, paramKind: String): String {
        val converterFieldName = converterFieldName(param.converterClass!!)
        val fallback = generateMissingFallback(param, paramName, paramKind)

        return if (fallback.isEmpty()) {
            """val ${param.name} = $extractor?.let { raw ->
    try { $converterFieldName.convert(raw) }
    catch (e: Exception) {
        throw BadRequestException("Invalid value for $paramKind param '$paramName': '${'$'}raw'. ${'$'}{e.message}")
    }
}"""
        } else {
            """val ${param.name} = $extractor?.let { raw ->
    try { $converterFieldName.convert(raw) }
    catch (e: Exception) {
        throw BadRequestException("Invalid value for $paramKind param '$paramName': '${'$'}raw'. ${'$'}{e.message}")
    }
}
    $fallback""".trimEnd()
        }
    }

    // --- Validation ---

    private fun nullGuard(name: String, nullable: Boolean): String =
        if (nullable) "$name != null && " else ""

    private fun generateValidation(param: ParamInfo): String {
        if (param.validations.isEmpty()) return ""

        val name = param.name
        val guard = nullGuard(name, param.nullable)

        return param.validations.joinToString("\n") { validation ->
            when (validation) {
                is ValidationInfo.Min ->
                    """if (${guard}$name < ${validation.value}) {
    throw BadRequestException("$name must be >= ${validation.value}, but was $$name")
}"""

                is ValidationInfo.Max ->
                    """if (${guard}$name > ${validation.value}) {
    throw BadRequestException("$name must be <= ${validation.value}, but was $$name")
}"""

                is ValidationInfo.NotBlank ->
                    """if ($name.isNullOrBlank()) {
    throw BadRequestException("$name must not be blank")
}"""

                is ValidationInfo.Size -> generateSizeValidation(param, validation)

                is ValidationInfo.Pattern ->
                    """if (${guard}!$name.matches(${name}Pattern)) {
    throw BadRequestException("$name must match pattern: ${validation.regex}")
}"""
            }
        }
    }

    private fun generateSizeValidation(param: ParamInfo, validation: ValidationInfo.Size): String {
        val name = param.name
        val isString = param.typeName == "String"
        val accessor = if (isString) "$name.length" else name
        val label = if (isString) "$name length" else name
        val condition = "$accessor < ${validation.min} || $accessor > ${validation.max}"
        val fullCondition = if (param.nullable) "$name != null && ($condition)" else condition

        return """if ($fullCondition) {
    throw BadRequestException("$label must be between ${validation.min} and ${validation.max}, but was $$accessor")
}"""
    }
}
