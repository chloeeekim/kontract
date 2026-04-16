package io.github.chloeeekim.kontract.processor

import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KontractCodeGeneratorTest {

    private fun param(
        name: String,
        typeName: String,
        source: ParamSource,
        nullable: Boolean = false,
        annotationName: String = "",
        defaultValue: String? = null,
        isEnum: Boolean = false,
        enumIgnoreCase: Boolean = false,
        qualifiedTypeName: String = typeName,
        validations: List<ValidationInfo> = emptyList(),
        converterClass: String? = null,
    ) = ParamInfo(
        name = name,
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

    private fun generate(params: List<ParamInfo>, className: String = "TestRequest", path: String = "/test") =
        KontractCodeGenerator.generate(
            packageName = "com.example",
            className = className,
            httpMethod = "GET",
            path = path,
            params = params,
        )

    // --- PathParam parsing ---

    @Test
    fun `should generate Long path param with let pattern`() {
        val code = generate(
            listOf(param("userId", "Long", ParamSource.PATH)),
            path = "/users/:userId",
        )

        assertContains(code, """ctx.pathParam("userId")?.let { raw ->""")
        assertContains(code, "raw.toLongOrNull()")
        assertContains(code, """throw BadRequestException("Invalid value for path param 'userId':""")
        assertContains(code, """throw BadRequestException("Missing path param: userId")""")
    }

    @Test
    fun `should generate Int path param with let pattern`() {
        val code = generate(
            listOf(param("page", "Int", ParamSource.PATH)),
            path = "/items/:page",
        )

        assertContains(code, """ctx.pathParam("page")?.let { raw ->""")
        assertContains(code, "raw.toIntOrNull()")
    }

    @Test
    fun `should generate String path param`() {
        val code = generate(
            listOf(param("slug", "String", ParamSource.PATH)),
            path = "/items/:slug",
        )

        assertContains(code, """ctx.pathParam("slug")""")
        assertContains(code, """throw BadRequestException("Missing path param: slug")""")
    }

    // --- QueryParam parsing ---

    @Test
    fun `should generate nullable query param`() {
        val code = generate(
            listOf(param("fields", "String", ParamSource.QUERY, nullable = true)),
        )

        assertContains(code, """ctx.request().getParam("fields")""")
        assertTrue(!code.contains("throw BadRequestException"))
    }

    @Test
    fun `should generate required query param`() {
        val code = generate(
            listOf(param("type", "String", ParamSource.QUERY)),
        )

        assertContains(code, """throw BadRequestException("Missing query param: type")""")
    }

    @Test
    fun `should generate Long query param with let pattern`() {
        val code = generate(
            listOf(param("limit", "Long", ParamSource.QUERY)),
        )

        assertContains(code, """ctx.request().getParam("limit")?.let { raw ->""")
        assertContains(code, "raw.toLongOrNull()")
        assertContains(code, """throw BadRequestException("Invalid value for query param 'limit':""")
        assertContains(code, """throw BadRequestException("Missing query param: limit")""")
    }

    @Test
    fun `should generate nullable Int query param with let pattern`() {
        val code = generate(
            listOf(param("page", "Int", ParamSource.QUERY, nullable = true)),
        )

        assertContains(code, """ctx.request().getParam("page")?.let { raw ->""")
        assertContains(code, "raw.toIntOrNull()")
        assertTrue(!code.contains("Missing query param"))
    }

    @Test
    fun `should use custom annotation name`() {
        val code = generate(
            listOf(param("user", "Long", ParamSource.PATH, annotationName = "userId")),
            path = "/users/:userId",
        )

        assertContains(code, """ctx.pathParam("userId")?.let { raw ->""")
        assertContains(code, "user = user,")
    }

    @Test
    fun `should generate multiple params`() {
        val code = generate(
            listOf(
                param("userId", "Long", ParamSource.PATH),
                param("fields", "String", ParamSource.QUERY, nullable = true),
                param("fieldNames", "String", ParamSource.QUERY, nullable = true),
            ),
            className = "GetUserPartialRequest",
            path = "/users/:userId/partial",
        )

        assertContains(code, "object GetUserPartialRequestContract")
        assertContains(code, """ctx.pathParam("userId")""")
        assertContains(code, """ctx.request().getParam("fields")""")
        assertContains(code, """ctx.request().getParam("fieldNames")""")
    }

    @Test
    fun `should include package and imports`() {
        val code = generate(
            listOf(param("id", "Long", ParamSource.PATH)),
            path = "/test/:id",
        )

        assertContains(code, "package com.example")
        assertContains(code, "import io.github.chloeeekim.kontract.annotation.BadRequestException")
        assertContains(code, "import io.vertx.ext.web.Router")
        assertContains(code, "import io.vertx.ext.web.RoutingContext")
    }

    @Test
    fun `should add import for Enum types`() {
        val code = generate(
            listOf(param("type", "TestType", ParamSource.QUERY, isEnum = true, qualifiedTypeName = "com.example.TestType")),
        )

        assertContains(code, "import com.example.TestType")
    }

    // --- Default values ---

    @Test
    fun `should generate default value for Int query param`() {
        val code = generate(
            listOf(param("limit", "Int", ParamSource.QUERY, defaultValue = "20")),
        )

        assertContains(code, "?: 20")
        assertTrue(!code.contains("Missing query param"))
    }

    @Test
    fun `should generate default value for Long query param`() {
        val code = generate(
            listOf(param("offset", "Long", ParamSource.QUERY, defaultValue = "0")),
        )

        assertContains(code, "?: 0")
    }

    @Test
    fun `should generate default value for String query param`() {
        val code = generate(
            listOf(param("sort", "String", ParamSource.QUERY, defaultValue = "asc")),
        )

        assertContains(code, """?: "asc"""")
    }

    @Test
    fun `should generate mixed required, nullable, and default params`() {
        val code = generate(
            listOf(
                param("type", "String", ParamSource.QUERY),
                param("page", "Int", ParamSource.QUERY, defaultValue = "1"),
                param("limit", "Int", ParamSource.QUERY, defaultValue = "20"),
                param("fields", "String", ParamSource.QUERY, nullable = true),
            ),
            className = "ListUsersRequest",
            path = "/users",
        )

        assertContains(code, """throw BadRequestException("Missing query param: type")""")
        assertContains(code, "?: 1")
        assertContains(code, "?: 20")
        assertTrue(!code.contains("""throw BadRequestException("Missing query param: fields")"""))
    }

    @Test
    fun `should escape special characters in String default value`() {
        val code = generate(
            listOf(param("greeting", "String", ParamSource.QUERY, defaultValue = """say "hello"""")),
        )

        assertContains(code, """?: "say \"hello\""""")
    }

    @Test
    fun `should escape backslash in String default value`() {
        val code = generate(
            listOf(param("path", "String", ParamSource.QUERY, defaultValue = """C:\temp""")),
        )

        assertContains(code, """?: "C:\\temp"""")
    }

    @Test
    fun `should escape dollar sign in String default value`() {
        val code = generate(
            listOf(param("template", "String", ParamSource.QUERY, defaultValue = "\${name}")),
        )

        assertContains(code, """?: "\${'$'}{name}"""")
    }

    @Test
    fun `should use default value when param is nullable with @Default`() {
        val code = generate(
            listOf(param("limit", "Int", ParamSource.QUERY, nullable = true, defaultValue = "20")),
        )

        assertContains(code, "?: 20")
    }

    // --- Enum parsing ---

    @Test
    fun `should generate required enum parsing with valueOf`() {
        val code = generate(
            listOf(param("type", "TestType", ParamSource.QUERY, isEnum = true, qualifiedTypeName = "com.example.TestType")),
        )

        assertContains(code, """ctx.request().getParam("type")?.let { raw ->""")
        assertContains(code, "com.example.TestType.valueOf(raw)")
        assertContains(code, """throw BadRequestException("Invalid value for query param 'type':""")
        assertContains(code, "Allowed:")
        assertContains(code, """throw BadRequestException("Missing query param: type")""")
    }

    @Test
    fun `should generate nullable enum parsing`() {
        val code = generate(
            listOf(param("type", "TestType", ParamSource.QUERY, nullable = true, isEnum = true, qualifiedTypeName = "com.example.TestType")),
        )

        assertContains(code, """ctx.request().getParam("type")?.let { raw ->""")
        assertContains(code, "com.example.TestType.valueOf(raw)")
        assertTrue(!code.contains("Missing query param"))
    }

    @Test
    fun `should generate enum parsing with @EnumIgnoreCase`() {
        val code = generate(
            listOf(param("type", "TestType", ParamSource.QUERY, nullable = true, isEnum = true, enumIgnoreCase = true, qualifiedTypeName = "com.example.TestType")),
        )

        assertContains(code, "com.example.TestType.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }")
        assertTrue(!code.contains("valueOf"))
    }

    @Test
    fun `should generate enum with default value using qualified name`() {
        val code = generate(
            listOf(param("sort", "SortOrder", ParamSource.QUERY, isEnum = true, qualifiedTypeName = "com.example.SortOrder", defaultValue = "SortOrder.ASC")),
        )

        assertContains(code, "com.example.SortOrder.valueOf(raw)")
        assertContains(code, "?: com.example.SortOrder.ASC")
    }

    @Test
    fun `should generate enum error message with allowed values`() {
        val code = generate(
            listOf(param("type", "TestType", ParamSource.QUERY, isEnum = true, qualifiedTypeName = "com.example.TestType")),
        )

        assertContains(code, "com.example.TestType.entries.joinToString()")
    }

    // --- Header Param ---

    @Test
    fun `should generate nullable header param`() {
        val code = generate(
            listOf(param("clientIp", "String", ParamSource.HEADER, nullable = true, annotationName = "X-Forwarded-For")),
        )

        assertContains(code, """ctx.request().getHeader("X-Forwarded-For")""")
        assertTrue(!code.contains("throw BadRequestException"))
    }

    @Test
    fun `should generate required header param`() {
        val code = generate(
            listOf(param("auth", "String", ParamSource.HEADER, annotationName = "Authorization")),
        )

        assertContains(code, """ctx.request().getHeader("Authorization")""")
        assertContains(code, """throw BadRequestException("Missing header param: Authorization")""")
    }

    @Test
    fun `should generate Int header param with let pattern`() {
        val code = generate(
            listOf(param("page", "Int", ParamSource.HEADER, annotationName = "X-Page")),
        )

        assertContains(code, """ctx.request().getHeader("X-Page")?.let { raw ->""")
        assertContains(code, "raw.toIntOrNull()")
        assertContains(code, """throw BadRequestException("Missing header param: X-Page")""")
    }

    // --- Cookie Param ---

    @Test
    fun `should generate nullable cookie param`() {
        val code = generate(
            listOf(param("sessionId", "String", ParamSource.COOKIE, nullable = true, annotationName = "session_id")),
        )

        assertContains(code, """ctx.request().getCookie("session_id")?.value""")
        assertTrue(!code.contains("throw BadRequestException"))
    }

    @Test
    fun `should generate required cookie param`() {
        val code = generate(
            listOf(param("token", "String", ParamSource.COOKIE, annotationName = "auth_token")),
        )

        assertContains(code, """ctx.request().getCookie("auth_token")?.value""")
        assertContains(code, """throw BadRequestException("Missing cookie param: auth_token")""")
    }

    // --- Body Param ---

    @Test
    fun `should generate body param with Jackson deserialization using singleton objectMapper`() {
        val code = generate(
            listOf(param("body", "AuthPayload", ParamSource.BODY, qualifiedTypeName = "com.example.AuthPayload")),
        )

        assertContains(code, "private val objectMapper = jacksonObjectMapper()")
        assertContains(code, "objectMapper.readValue(ctx.body().buffer().bytes, AuthPayload::class.java)")
        assertTrue(!code.contains("jacksonObjectMapper().readValue"))
        assertContains(code, """throw BadRequestException("Invalid request body:""")
    }

    @Test
    fun `should add imports for body param`() {
        val code = generate(
            listOf(param("body", "AuthPayload", ParamSource.BODY, qualifiedTypeName = "com.example.AuthPayload")),
        )

        assertContains(code, "import com.example.AuthPayload")
        assertContains(code, "import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper")
    }

    @Test
    fun `should generate body param with kotlinx serialization`() {
        val code = KontractCodeGenerator.generate(
            packageName = "com.example",
            className = "TestRequest",
            httpMethod = "POST",
            path = "/test",
            params = listOf(param("body", "AuthPayload", ParamSource.BODY, qualifiedTypeName = "com.example.AuthPayload")),
            serializerMode = SerializerMode.KOTLINX,
        )

        assertContains(code, "Json.decodeFromString<AuthPayload>(ctx.body().asString())")
        assertContains(code, "import kotlinx.serialization.json.Json")
        assertContains(code, "// AuthPayload must be annotated with @kotlinx.serialization.Serializable")
        assertTrue(!code.contains("objectMapper"))
        assertTrue(!code.contains("jacksonObjectMapper"))
    }

    @Test
    fun `should not generate objectMapper field when using kotlinx`() {
        val code = KontractCodeGenerator.generate(
            packageName = "com.example",
            className = "TestRequest",
            httpMethod = "POST",
            path = "/test",
            params = listOf(param("body", "AuthPayload", ParamSource.BODY, qualifiedTypeName = "com.example.AuthPayload")),
            serializerMode = SerializerMode.KOTLINX,
        )

        assertTrue(!code.contains("private val objectMapper"))
    }

    // --- route() method ---

    @Test
    fun `should generate route method with GET`() {
        val code = generate(
            listOf(param("id", "Long", ParamSource.PATH)),
            className = "GetUserRequest",
            path = "/users/:id",
        )

        assertContains(code, "fun route(router: Router, handler: (GetUserRequest, RoutingContext) -> Unit)")
        assertContains(code, """router.get("/users/:id").handler { ctx ->""")
        assertContains(code, "val request = from(ctx)")
        assertContains(code, "handler(request, ctx)")
    }

    @Test
    fun `should generate route method with POST`() {
        val code = KontractCodeGenerator.generate(
            packageName = "com.example",
            className = "CreateUserRequest",
            httpMethod = "POST",
            path = "/users",
            params = listOf(param("name", "String", ParamSource.QUERY)),
        )

        assertContains(code, """router.post("/users").handler { ctx ->""")
    }

    @Test
    fun `should generate route method with PUT`() {
        val code = KontractCodeGenerator.generate(
            packageName = "com.example",
            className = "UpdateUserRequest",
            httpMethod = "PUT",
            path = "/users/:id",
            params = listOf(param("id", "Long", ParamSource.PATH)),
        )

        assertContains(code, """router.put("/users/:id").handler { ctx ->""")
    }

    @Test
    fun `should generate route method with DELETE`() {
        val code = KontractCodeGenerator.generate(
            packageName = "com.example",
            className = "DeleteUserRequest",
            httpMethod = "DELETE",
            path = "/users/:id",
            params = listOf(param("id", "Long", ParamSource.PATH)),
        )

        assertContains(code, """router.delete("/users/:id").handler { ctx ->""")
    }

    @Test
    fun `should generate route method with PATCH`() {
        val code = KontractCodeGenerator.generate(
            packageName = "com.example",
            className = "PatchUserRequest",
            httpMethod = "PATCH",
            path = "/users/:id",
            params = listOf(param("id", "Long", ParamSource.PATH)),
        )

        assertContains(code, """router.patch("/users/:id").handler { ctx ->""")
    }

    @Test
    fun `should generate route with try-catch for BadRequestException`() {
        val code = generate(
            listOf(param("id", "Long", ParamSource.PATH)),
            className = "TestRequest",
            path = "/test/:id",
        )

        assertContains(code, "try {")
        assertContains(code, "val request = from(ctx)")
        assertContains(code, "} catch (e: BadRequestException) {")
        assertContains(code, "KontractConfig.errorHandler.handleError(ctx, e)")
    }

    @Test
    fun `should import Router`() {
        val code = generate(
            listOf(param("id", "Long", ParamSource.PATH)),
            path = "/test/:id",
        )

        assertContains(code, "import io.vertx.ext.web.Router")
    }

    @Test
    fun `should import ContractConfig`() {
        val code = generate(
            listOf(param("id", "Long", ParamSource.PATH)),
            path = "/test/:id",
        )

        assertContains(code, "import io.github.chloeeekim.kontract.annotation.KontractConfig")
    }

    // --- Response type ---

    @Test
    fun `should generate typed route overload when response type specified`() {
        val code = KontractCodeGenerator.generate(
            packageName = "com.example",
            className = "GetUserRequest",
            httpMethod = "GET",
            path = "/users/:id",
            params = listOf(param("id", "Long", ParamSource.PATH)),
            responseType = "com.example.UserResponse",
            statusCode = 200,
        )

        assertContains(code, "fun routeWithResponse(router: Router, handler: (GetUserRequest, RoutingContext) -> UserResponse)")
        assertContains(code, "val response = handler(request, ctx)")
        assertContains(code, ".setStatusCode(200)")
        assertContains(code, """putHeader("Content-Type", "application/json")""")
        assertContains(code, "objectMapper.writeValueAsString(response)")
        // Unit overload
        assertContains(code, "fun route(router: Router, handler: (GetUserRequest, RoutingContext) -> Unit)")
    }

    @Test
    fun `should not generate typed route when no response type`() {
        val code = generate(
            listOf(param("id", "Long", ParamSource.PATH)),
            path = "/test/:id",
        )

        assertTrue(!code.contains("-> UserResponse"))
        assertTrue(!code.contains("writeValueAsString"))
        // Unit overload
        assertContains(code, "-> Unit)")
    }

    @Test
    fun `should generate typed route with custom status code`() {
        val code = KontractCodeGenerator.generate(
            packageName = "com.example",
            className = "CreateUserRequest",
            httpMethod = "POST",
            path = "/users",
            params = listOf(param("name", "String", ParamSource.QUERY)),
            responseType = "com.example.UserResponse",
            statusCode = 201,
        )

        assertContains(code, ".setStatusCode(201)")
    }

    @Test
    fun `should generate typed route with kotlinx serialization`() {
        val code = KontractCodeGenerator.generate(
            packageName = "com.example",
            className = "GetUserRequest",
            httpMethod = "GET",
            path = "/users/:id",
            params = listOf(param("id", "Long", ParamSource.PATH)),
            serializerMode = SerializerMode.KOTLINX,
            responseType = "com.example.UserResponse",
        )

        assertContains(code, "Json.encodeToString(response)")
        assertTrue(!code.contains("objectMapper"))
    }

    @Test
    fun `should import response type`() {
        val code = KontractCodeGenerator.generate(
            packageName = "com.example",
            className = "GetUserRequest",
            httpMethod = "GET",
            path = "/users/:id",
            params = listOf(param("id", "Long", ParamSource.PATH)),
            responseType = "com.example.UserResponse",
        )

        assertContains(code, "import com.example.UserResponse")
    }

    @Test
    fun `should generate objectMapper when response type but no BodyParam`() {
        val code = KontractCodeGenerator.generate(
            packageName = "com.example",
            className = "GetUserRequest",
            httpMethod = "GET",
            path = "/users/:id",
            params = listOf(param("id", "Long", ParamSource.PATH)),
            responseType = "com.example.UserResponse",
        )

        assertContains(code, "private val objectMapper = jacksonObjectMapper()")
    }

    @Test
    fun `should skip body serialization for 204 status code`() {
        val code = KontractCodeGenerator.generate(
            packageName = "com.example",
            className = "DeleteUserRequest",
            httpMethod = "DELETE",
            path = "/users/:id",
            params = listOf(param("id", "Long", ParamSource.PATH)),
            responseType = "com.example.EmptyResponse",
            statusCode = 204,
        )

        assertContains(code, ".setStatusCode(204).end()")
        assertTrue(!code.contains("writeValueAsString"))
        assertTrue(!code.contains("encodeToString"))
        assertTrue(!code.contains("Content-Type"))
    }

    @Test
    fun `should skip body serialization for 304 status code`() {
        val code = KontractCodeGenerator.generate(
            packageName = "com.example",
            className = "CheckRequest",
            httpMethod = "GET",
            path = "/check",
            params = listOf(param("id", "Long", ParamSource.QUERY)),
            responseType = "com.example.CheckResponse",
            statusCode = 304,
        )

        assertContains(code, ".setStatusCode(304).end()")
        assertTrue(!code.contains("writeValueAsString"))
    }

    // --- Validation ---

    @Test
    fun `should generate @Min validation`() {
        val code = generate(
            listOf(param("userId", "Long", ParamSource.PATH,
                validations = listOf(ValidationInfo.Min(1)))),
            path = "/users/:userId",
        )

        assertContains(code, """if (userId < 1)""")
        assertContains(code, """"userId must be >= 1, but was ${'$'}userId"""")
    }

    @Test
    fun `should generate @Max validation`() {
        val code = generate(
            listOf(param("limit", "Int", ParamSource.QUERY,
                validations = listOf(ValidationInfo.Max(100)))),
        )

        assertContains(code, """if (limit > 100)""")
        assertContains(code, """"limit must be <= 100, but was ${'$'}limit"""")
    }

    @Test
    fun `should generate @NotBlank validation`() {
        val code = generate(
            listOf(param("name", "String", ParamSource.QUERY,
                validations = listOf(ValidationInfo.NotBlank))),
        )

        assertContains(code, """if (name.isNullOrBlank())""")
        assertContains(code, """"name must not be blank"""")
    }

    @Test
    fun `should generate @Size validation`() {
        val code = generate(
            listOf(param("limit", "Int", ParamSource.QUERY,
                validations = listOf(ValidationInfo.Size(1, 100)))),
        )

        assertContains(code, """if (limit < 1 || limit > 100)""")
        assertContains(code, """"limit must be between 1 and 100, but was ${'$'}limit"""")
    }

    @Test
    fun `should generate @Size validation for String using length`() {
        val code = generate(
            listOf(param("name", "String", ParamSource.QUERY,
                validations = listOf(ValidationInfo.Size(1, 50)))),
        )

        assertContains(code, """if (name.length < 1 || name.length > 50)""")
        assertContains(code, """"name length must be between 1 and 50""")
    }

    @Test
    fun `should generate nullable @Size validation for String`() {
        val code = generate(
            listOf(param("name", "String", ParamSource.QUERY, nullable = true,
                validations = listOf(ValidationInfo.Size(1, 50)))),
        )

        assertContains(code, """if (name != null && (name.length < 1 || name.length > 50))""")
    }

    @Test
    fun `should generate @Pattern validation`() {
        val code = generate(
            listOf(param("fields", "String", ParamSource.QUERY, nullable = true,
                validations = listOf(ValidationInfo.Pattern("[a-zA-Z,]+")))),
        )

        assertContains(code, """if (fields != null && !fields.matches(fieldsPattern))""")
        assertContains(code, """"fields must match pattern: [a-zA-Z,]+"""")
    }

    @Test
    fun `should precompile Regex as private val field`() {
        val code = generate(
            listOf(param("fields", "String", ParamSource.QUERY, nullable = true,
                validations = listOf(ValidationInfo.Pattern("[a-zA-Z,]+")))),
        )

        assertContains(code, """private val fieldsPattern = Regex("[a-zA-Z,]+")""")
        assertTrue(!code.contains("""Regex("[a-zA-Z,]+")""".let { "matches($it)" }))
    }

    @Test
    fun `should not generate Regex field when no @Pattern validation`() {
        val code = generate(
            listOf(param("limit", "Int", ParamSource.QUERY,
                validations = listOf(ValidationInfo.Min(1)))),
        )

        assertTrue(!code.contains("private val") || !code.contains("Pattern"))
    }

    @Test
    fun `should generate nullable @Min validation with null check`() {
        val code = generate(
            listOf(param("score", "Int", ParamSource.QUERY, nullable = true,
                validations = listOf(ValidationInfo.Min(0)))),
        )

        assertContains(code, """if (score != null && score < 0)""")
    }

    @Test
    fun `should generate multiple validations`() {
        val code = generate(
            listOf(param("limit", "Int", ParamSource.QUERY,
                validations = listOf(ValidationInfo.Min(1), ValidationInfo.Max(100)))),
        )

        assertContains(code, """if (limit < 1)""")
        assertContains(code, """if (limit > 100)""")
    }

    // --- Companion extensions ---

    @Test
    fun `should generate companion extensions with from and route`() {
        val code = KontractCodeGenerator.generateCompanionExtensions(
            packageName = "com.example",
            className = "GetUserRequest",
        )

        assertContains(code, "fun GetUserRequest.Companion.from(ctx: RoutingContext)")
        assertContains(code, "GetUserRequestContract.from(ctx)")
        assertContains(code, "fun GetUserRequest.Companion.route(router: Router, handler: (GetUserRequest, RoutingContext) -> Unit)")
        assertContains(code, "GetUserRequestContract.route(router, handler)")
    }

    @Test
    fun `should generate routeWithResponse extension when response type specified`() {
        val code = KontractCodeGenerator.generateCompanionExtensions(
            packageName = "com.example",
            className = "GetUserRequest",
            responseType = "com.example.UserResponse",
        )

        assertContains(code, "fun GetUserRequest.Companion.routeWithResponse(router: Router, handler: (GetUserRequest, RoutingContext) -> UserResponse)")
        assertContains(code, "GetUserRequestContract.routeWithResponse(router, handler)")
    }

    @Test
    fun `should not generate routeWithResponse extension when no response type`() {
        val code = KontractCodeGenerator.generateCompanionExtensions(
            packageName = "com.example",
            className = "GetUserRequest",
        )

        assertTrue(!code.contains("routeWithResponse"))
    }

    @Test
    fun `should import response type in companion extensions`() {
        val code = KontractCodeGenerator.generateCompanionExtensions(
            packageName = "com.example",
            className = "GetUserRequest",
            responseType = "com.other.UserResponse",
        )

        assertContains(code, "import com.other.UserResponse")
    }

    @Test
    fun `should use named companion in extensions`() {
        val code = KontractCodeGenerator.generateCompanionExtensions(
            packageName = "com.example",
            className = "GetUserRequest",
            companionName = "Factory",
        )

        assertContains(code, "fun GetUserRequest.Factory.from(ctx: RoutingContext)")
        assertContains(code, "fun GetUserRequest.Factory.route(router: Router")
        assertTrue(!code.contains("GetUserRequest.Companion"))
    }

    // --- @TypeConverter tests ---

    @Test
    fun `should generate converter parsing for query param`() {
        val code = generate(
            listOf(
                param(
                    "startDate", "LocalDate",
                    source = ParamSource.QUERY,
                    qualifiedTypeName = "java.time.LocalDate",
                    converterClass = "io.vertx.contract.annotation.converter.LocalDateConverter",
                ),
            ),
            path = "/events",
        )

        assertContains(code, "private val localDateConverter = LocalDateConverter()")
        assertContains(code, "localDateConverter.convert(raw)")
        assertTrue(!code.contains("LocalDateConverter().convert(raw)"))
        assertContains(code, "import io.vertx.contract.annotation.converter.LocalDateConverter")
        assertContains(code, "import java.time.LocalDate")
        assertContains(code, """throw BadRequestException("Missing query param: startDate")""")
    }

    @Test
    fun `should generate converter parsing for nullable param`() {
        val code = generate(
            listOf(
                param(
                    "eventDate", "LocalDate",
                    source = ParamSource.QUERY,
                    nullable = true,
                    qualifiedTypeName = "java.time.LocalDate",
                    converterClass = "io.vertx.contract.annotation.converter.LocalDateConverter",
                ),
            ),
            path = "/events",
        )

        assertContains(code, "localDateConverter.convert(raw)")
        assertTrue(!code.contains("Missing query param"))
    }

    @Test
    fun `should generate converter parsing for path param`() {
        val code = generate(
            listOf(
                param(
                    "id", "UUID",
                    source = ParamSource.PATH,
                    qualifiedTypeName = "java.util.UUID",
                    converterClass = "io.vertx.contract.annotation.converter.UUIDConverter",
                ),
            ),
            path = "/items/:id",
        )

        assertContains(code, "uUIDConverter.convert(raw)")
        assertContains(code, """ctx.pathParam("id")?.let { raw ->""")
        assertContains(code, "import io.vertx.contract.annotation.converter.UUIDConverter")
        assertContains(code, "import java.util.UUID")
    }

    @Test
    fun `should generate converter parsing for header param`() {
        val code = generate(
            listOf(
                param(
                    "requestDate", "LocalDate",
                    source = ParamSource.HEADER,
                    annotationName = "X-Request-Date",
                    qualifiedTypeName = "java.time.LocalDate",
                    converterClass = "io.vertx.contract.annotation.converter.LocalDateConverter",
                ),
            ),
            path = "/test",
        )

        assertContains(code, """ctx.request().getHeader("X-Request-Date")?.let { raw ->""")
        assertContains(code, "localDateConverter.convert(raw)")
    }

    @Test
    fun `should generate converter parsing for cookie param`() {
        val code = generate(
            listOf(
                param(
                    "sessionExpiry", "LocalDate",
                    source = ParamSource.COOKIE,
                    annotationName = "session_expiry",
                    nullable = true,
                    qualifiedTypeName = "java.time.LocalDate",
                    converterClass = "io.vertx.contract.annotation.converter.LocalDateConverter",
                ),
            ),
            path = "/test",
        )

        assertContains(code, """ctx.request().getCookie("session_expiry")?.value?.let { raw ->""")
        assertContains(code, "localDateConverter.convert(raw)")
        assertTrue(!code.contains("Missing cookie param"))
    }

    @Test
    fun `should generate converter with error message containing param info`() {
        val code = generate(
            listOf(
                param(
                    "amount", "BigDecimal",
                    source = ParamSource.QUERY,
                    qualifiedTypeName = "java.math.BigDecimal",
                    converterClass = "io.vertx.contract.annotation.converter.BigDecimalConverter",
                ),
            ),
            path = "/payments",
        )

        assertContains(code, """throw BadRequestException("Invalid value for query param 'amount':""")
    }

    @Test
    fun `should generate single converter field for same converter used on multiple params`() {
        val code = generate(
            listOf(
                param(
                    "startDate", "LocalDate",
                    source = ParamSource.QUERY,
                    nullable = true,
                    qualifiedTypeName = "java.time.LocalDate",
                    converterClass = "io.vertx.contract.annotation.converter.LocalDateConverter",
                ),
                param(
                    "endDate", "LocalDate",
                    source = ParamSource.QUERY,
                    nullable = true,
                    qualifiedTypeName = "java.time.LocalDate",
                    converterClass = "io.vertx.contract.annotation.converter.LocalDateConverter",
                ),
            ),
            path = "/events",
        )

        val count = Regex("private val localDateConverter = LocalDateConverter\\(\\)")
            .findAll(code).count()
        assertEquals(1, count, "Only one should be generated for the same converter class.")
    }
}
