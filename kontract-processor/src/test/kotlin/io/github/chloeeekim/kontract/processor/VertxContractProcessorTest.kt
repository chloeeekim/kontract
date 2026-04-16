package io.github.chloeeekim.kontract.processor

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.kspProcessorOptions
import com.tschuchort.compiletesting.kspSourcesDir
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class VertxContractProcessorTest {

    @Test
    fun `should generate contract for simple GET endpoint`() {
        val generated = compileAndFindSource(
            source("GetUserRequest", """
                @PathParam val userId: Long,
                @QueryParam val fields: String? = null,
            """, path = "/users/:userId"),
            "GetUserRequestContract.kt",
        )

        assertContains(generated, "object GetUserRequestContract")
        assertContains(generated, "fun from(ctx: RoutingContext): GetUserRequest")
        assertContains(generated, """ctx.pathParam("userId")?.let { raw ->""")
        assertContains(generated, """ctx.request().getParam("fields")""")
    }

    @Test
    fun `should generate contract with Int path param`() {
        val generated = compileAndFindSource(
            source("PageRequest", "@PathParam val page: Int,", path = "/pages/:page"),
            "PageRequestContract.kt",
        )

        assertContains(generated, """ctx.pathParam("page")?.let { raw ->""")
        assertContains(generated, "raw.toIntOrNull()")
    }

    @Test
    fun `should generate contract with custom param name`() {
        val generated = compileAndFindSource(
            source("UserRequest", """@PathParam(name = "userId") val user: Long,""", path = "/users/:userId"),
            "UserRequestContract.kt",
        )

        assertContains(generated, """ctx.pathParam("userId")?.let { raw ->""")
        assertContains(generated, "user = user,")
    }

    @Test
    fun `should generate contract with required query param`() {
        val generated = compileAndFindSource(
            source("ItemRequest", "@QueryParam val type: String,", path = "/items"),
            "ItemRequestContract.kt",
        )

        assertContains(generated, """throw BadRequestException("Missing query param: type")""")
    }

    @Test
    fun `should generate contract with multiple params`() {
        val generated = compileAndFindSource(
            source("GetUserPartialRequest", """
                @PathParam val userId: Long,
                @QueryParam val fields: String? = null,
                @QueryParam val fieldNames: String? = null,
            """, path = "/users/:userId/partial"),
            "GetUserPartialRequestContract.kt",
        )

        assertContains(generated, "object GetUserPartialRequestContract")
        assertContains(generated, """ctx.pathParam("userId")""")
        assertContains(generated, """ctx.request().getParam("fields")""")
        assertContains(generated, """ctx.request().getParam("fieldNames")""")
    }

    @Test
    fun `should include correct package and imports`() {
        val generated = compileAndFindSource(
            source("TestRequest", "@PathParam val id: Long,", path = "/test/:id"),
            "TestRequestContract.kt",
        )

        assertContains(generated, "package com.example")
        assertContains(generated, "import io.github.chloeeekim.kontract.annotation.BadRequestException")
        assertContains(generated, "import io.vertx.ext.web.RoutingContext")
    }

    @Test
    fun `should generate contract with @Default values`() {
        val generated = compileAndFindSource(
            source("ListUsersRequest", """
                @QueryParam @Default("1") val page: Int = 1,
                @QueryParam @Default("20") val limit: Int = 20,
                @QueryParam val fields: String? = null,
                @QueryParam val type: String,
            """, path = "/users"),
            "ListUsersRequestContract.kt",
        )

        assertContains(generated, "?: 1")
        assertContains(generated, "?: 20")
        assertTrue(!generated.contains("""throw BadRequestException("Missing query param: fields")"""))
        assertContains(generated, """throw BadRequestException("Missing query param: type")""")
    }

    @Test
    fun `should warn when @Default is used on PathParam`() {
        val (result, _) = compileWithResult(
            source("UserRequest", """@PathParam @Default("1") val userId: Long = 1,""", path = "/users/:userId"),
        )

        assertContains(result.messages, "@Default on @PathParam 'userId' is discouraged")
    }

    @Test
    fun `should generate contract with nullable + @Default combo`() {
        val generated = compileAndFindSource(
            source("ItemRequest", """@QueryParam @Default("20") val limit: Int? = 20,""", path = "/items"),
            "ItemRequestContract.kt",
        )

        assertContains(generated, "?: 20")
    }

    // --- Enum tests ---

    @Test
    fun `should generate contract with required enum param`() {
        val src = SourceFile.kotlin("TestRequest.kt", """
            package com.example

            import io.github.chloeeekim.kontract.annotation.*

            enum class TestType { T1, T2, T3 }

            @VertxEndpoint(method = HttpMethod.GET, path = "/test")
            data class TestRequest(
                @QueryParam val type: TestType,
            )
        """)

        val generated = compileAndFindSource(src, "TestRequestContract.kt")

        assertContains(generated, "TestType.valueOf(raw)")
        assertContains(generated, """throw BadRequestException("Missing query param: type")""")
        assertContains(generated, "TestType.entries.joinToString()")
    }

    @Test
    fun `should generate contract with nullable enum param`() {
        val src = SourceFile.kotlin("TestRequest.kt", """
            package com.example

            import io.github.chloeeekim.kontract.annotation.*

            enum class TestType { T1, T2, T3 }

            @VertxEndpoint(method = HttpMethod.GET, path = "/test")
            data class TestRequest(
                @QueryParam val type: TestType? = null,
            )
        """)

        val generated = compileAndFindSource(src, "TestRequestContract.kt")

        assertContains(generated, "TestType.valueOf(raw)")
        assertTrue(!generated.contains("Missing query param"))
    }

    @Test
    fun `should generate contract with @EnumIgnoreCase`() {
        val src = SourceFile.kotlin("TestRequest.kt", """
            package com.example

            import io.github.chloeeekim.kontract.annotation.*

            enum class TestType { T1, T2, T3 }

            @VertxEndpoint(method = HttpMethod.GET, path = "/test")
            data class TestRequest(
                @QueryParam @EnumIgnoreCase val type: TestType? = null,
            )
        """)

        val generated = compileAndFindSource(src, "TestRequestContract.kt")

        assertContains(generated, "TestType.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }")
        assertTrue(!generated.contains("valueOf"))
    }

    @Test
    fun `should generate contract with enum + @Default`() {
        val src = SourceFile.kotlin("TestRequest.kt", """
            package com.example

            import io.github.chloeeekim.kontract.annotation.*

            enum class SortOrder { ASC, DESC }

            @VertxEndpoint(method = HttpMethod.GET, path = "/items")
            data class ItemRequest(
                @QueryParam @Default("SortOrder.ASC") val sort: SortOrder = SortOrder.ASC,
            )
        """)

        val generated = compileAndFindSource(src, "ItemRequestContract.kt")

        assertContains(generated, "SortOrder.valueOf(raw)")
        assertContains(generated, "?: com.example.SortOrder.ASC")
    }

    @Test
    fun `should generate contract with mixed enum params matching design doc`() {
        val src = SourceFile.kotlin("TestRequest.kt", """
            package com.example

            import io.github.chloeeekim.kontract.annotation.*

            enum class TestType { T1, T2, T3 }
            enum class SortOrder { ASC, DESC }

            @VertxEndpoint(method = HttpMethod.GET, path = "/test/:id")
            data class TestRequest(
                @PathParam val id: Long,
                @QueryParam val type: TestType,
                @QueryParam @Default("SortOrder.ASC") val sort: SortOrder = SortOrder.ASC,
                @QueryParam @EnumIgnoreCase val status: TestType? = null,
            )
        """)

        val generated = compileAndFindSource(src, "TestRequestContract.kt")

        // id — Long path param
        assertContains(generated, """ctx.pathParam("id")?.let { raw ->""")
        // type — required enum
        assertContains(generated, "TestType.valueOf(raw)")
        assertContains(generated, """throw BadRequestException("Missing query param: type")""")
        // sort — enum with default
        assertContains(generated, "SortOrder.valueOf(raw)")
        assertContains(generated, "?: com.example.SortOrder.ASC")
        // status — nullable enum with ignore case
        assertContains(generated, "TestType.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }")
    }

    @Test
    fun `should error when @Default value is invalid for Int`() {
        val src = SourceFile.kotlin("TestRequest.kt", """
            package com.example

            import io.github.chloeeekim.kontract.annotation.*

            @VertxEndpoint(method = HttpMethod.GET, path = "/items")
            data class ItemRequest(
                @QueryParam @Default("abc") val limit: Int = 0,
            )
        """)

        val (result, _) = compileWithResult(src)

        assertContains(result.messages, "@Default value 'abc' is not a valid Int for 'limit'")
    }

    @Test
    fun `should error when @Default value is invalid for enum`() {
        val src = SourceFile.kotlin("TestRequest.kt", """
            package com.example

            import io.github.chloeeekim.kontract.annotation.*

            enum class SortOrder { ASC, DESC }

            @VertxEndpoint(method = HttpMethod.GET, path = "/items")
            data class ItemRequest(
                @QueryParam @Default("SortOrder.INVALID") val sort: SortOrder = SortOrder.ASC,
            )
        """)

        val (result, _) = compileWithResult(src)

        assertContains(result.messages, "@Default value 'SortOrder.INVALID' is not a valid SortOrder entry for 'sort'")
    }

    // --- route() method ---

    @Test
    fun `should generate route method for GET endpoint`() {
        val generated = compileAndFindSource(
            source("GetUserRequest", "@PathParam val userId: Long,", path = "/users/:userId"),
            "GetUserRequestContract.kt",
        )

        assertContains(generated, "fun route(router: Router, handler: (GetUserRequest, RoutingContext) -> Unit)")
        assertContains(generated, """router.get("/users/:userId").handler { ctx ->""")
        assertContains(generated, "try {")
        assertContains(generated, "val request = from(ctx)")
        assertContains(generated, "handler(request, ctx)")
        assertContains(generated, "} catch (e: BadRequestException) {")
        assertContains(generated, "ctx.response().setStatusCode(400).end(e.message)")
    }

    @Test
    fun `should generate route method for POST endpoint`() {
        val src = SourceFile.kotlin("CreateUserRequest.kt", """
            package com.example

            import io.github.chloeeekim.kontract.annotation.*

            @VertxEndpoint(method = HttpMethod.POST, path = "/users")
            data class CreateUserRequest(
                @QueryParam val name: String,
            )
        """)

        val generated = compileAndFindSource(src, "CreateUserRequestContract.kt")

        assertContains(generated, """router.post("/users").handler { ctx ->""")
    }

    // --- Header / Cookie / Body ---

    @Test
    fun `should generate contract with HeaderParam`() {
        val generated = compileAndFindSource(
            source("TestRequest", """
                @HeaderParam("X-Forwarded-For") val clientIp: String? = null,
                @HeaderParam("User-Agent") val userAgent: String? = null,
            """),
            "TestRequestContract.kt",
        )

        assertContains(generated, """ctx.request().getHeader("X-Forwarded-For")""")
        assertContains(generated, """ctx.request().getHeader("User-Agent")""")
    }

    @Test
    fun `should generate contract with CookieParam`() {
        val generated = compileAndFindSource(
            source("TestRequest", """@CookieParam("session_id") val sessionId: String? = null,"""),
            "TestRequestContract.kt",
        )

        assertContains(generated, """ctx.request().getCookie("session_id")?.value""")
    }

    @Test
    fun `should generate contract with BodyParam`() {
        val src = SourceFile.kotlin("TestRequest.kt", """
            package com.example

            import io.github.chloeeekim.kontract.annotation.*

            data class AuthPayload(
                val macAddress: String,
                val serialNumber: String,
            )

            @VertxEndpoint(method = HttpMethod.POST, path = "/auth")
            data class AuthRequest(
                @HeaderParam("X-Forwarded-For") val clientIp: String? = null,
                @CookieParam("session_id") val sessionId: String? = null,
                @BodyParam val body: AuthPayload,
            )
        """)

        val generated = compileAndFindSource(src, "AuthRequestContract.kt")

        assertContains(generated, """ctx.request().getHeader("X-Forwarded-For")""")
        assertContains(generated, """ctx.request().getCookie("session_id")?.value""")
        assertContains(generated, "objectMapper.readValue(ctx.body().buffer().bytes, AuthPayload::class.java)")
        assertContains(generated, """throw BadRequestException("Invalid request body:""")
    }

    @Test
    fun `should add Enum import in generated code`() {
        val src = SourceFile.kotlin("TestRequest.kt", """
            package com.example

            import io.github.chloeeekim.kontract.annotation.*

            enum class TestType { T1, T2 }

            @VertxEndpoint(method = HttpMethod.GET, path = "/test")
            data class TestRequest(
                @QueryParam val type: TestType,
            )
        """)

        val generated = compileAndFindSource(src, "TestRequestContract.kt")

        assertContains(generated, "import com.example.TestType")
    }

    // --- Response type ---

    @Test
    fun `should generate typed route when response specified`() {
        val src = SourceFile.kotlin("TestRequest.kt", """
            package com.example

            import io.github.chloeeekim.kontract.annotation.*

            data class UserResponse(val id: Long, val name: String)

            @VertxEndpoint(method = HttpMethod.GET, path = "/users/:userId", response = UserResponse::class)
            data class GetUserRequest(
                @PathParam val userId: Long,
            )
        """)

        val generated = compileAndFindSource(src, "GetUserRequestContract.kt")

        assertContains(generated, "fun routeWithResponse(router: Router, handler: (GetUserRequest, RoutingContext) -> UserResponse)")
        assertContains(generated, "objectMapper.writeValueAsString(response)")
        assertContains(generated, ".setStatusCode(200)")
        // Unit overload
        assertContains(generated, "fun route(router: Router, handler: (GetUserRequest, RoutingContext) -> Unit)")
    }

    @Test
    fun `should generate typed route with custom status code`() {
        val src = SourceFile.kotlin("TestRequest.kt", """
            package com.example

            import io.github.chloeeekim.kontract.annotation.*

            data class UserResponse(val id: Long, val name: String)

            @VertxEndpoint(method = HttpMethod.POST, path = "/users", response = UserResponse::class, statusCode = 201)
            data class CreateUserRequest(
                @QueryParam val name: String,
            )
        """)

        val generated = compileAndFindSource(src, "CreateUserRequestContract.kt")

        assertContains(generated, ".setStatusCode(201)")
    }

    @Test
    fun `should not generate typed route when no response`() {
        val generated = compileAndFindSource(
            source("TestRequest", "@PathParam val id: Long,", path = "/test/:id"),
            "TestRequestContract.kt",
        )

        assertContains(generated, "-> Unit)")
        // If the response is not specified, the typed route should not be generated
        assertTrue(!generated.contains("-> Nothing)"), "Should not contain Nothing typed route: $generated")
        assertTrue(!generated.contains("val response = handler"), "Should not contain response handler: $generated")
    }

    // --- Companion extensions ---

    @Test
    fun `should generate companion extensions when companion object exists`() {
        val src = SourceFile.kotlin("TestRequest.kt", """
            package com.example

            import io.github.chloeeekim.kontract.annotation.*

            @VertxEndpoint(method = HttpMethod.GET, path = "/test/:id")
            data class TestRequest(
                @PathParam val id: Long,
            ) {
                companion object
            }
        """)

        val (_, compilation) = compileWithResult(src)
        val extensions = findGeneratedSource(compilation, "TestRequestExtensions.kt")

        assertContains(extensions, "fun TestRequest.Companion.from(ctx: RoutingContext)")
        assertContains(extensions, "fun TestRequest.Companion.route(router: Router")
    }

    @Test
    fun `should generate routeWithResponse companion extension when response specified`() {
        val src = SourceFile.kotlin("TestRequest.kt", """
            package com.example

            import io.github.chloeeekim.kontract.annotation.*

            data class UserResponse(val id: Long)

            @VertxEndpoint(method = HttpMethod.GET, path = "/users/:id", response = UserResponse::class)
            data class GetUserRequest(
                @PathParam val id: Long,
            ) {
                companion object
            }
        """)

        val (_, compilation) = compileWithResult(src)
        val extensions = findGeneratedSource(compilation, "GetUserRequestExtensions.kt")

        assertContains(extensions, "fun GetUserRequest.Companion.routeWithResponse(router: Router")
    }

    @Test
    fun `should not generate extensions when no companion object`() {
        val src = SourceFile.kotlin("TestRequest.kt", """
            package com.example

            import io.github.chloeeekim.kontract.annotation.*

            @VertxEndpoint(method = HttpMethod.GET, path = "/test/:id")
            data class TestRequest(
                @PathParam val id: Long,
            )
        """)

        val (_, compilation) = compileWithResult(src)
        val generatedFiles = compilation.kspSourcesDir
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

        assertTrue(generatedFiles.none { it.name == "TestRequestExtensions.kt" })
    }

    @Test
    fun `should use named companion in extensions`() {
        val src = SourceFile.kotlin("TestRequest.kt", """
            package com.example

            import io.github.chloeeekim.kontract.annotation.*

            @VertxEndpoint(method = HttpMethod.GET, path = "/test/:id")
            data class TestRequest(
                @PathParam val id: Long,
            ) {
                companion object Factory
            }
        """)

        val (_, compilation) = compileWithResult(src)
        val extensions = findGeneratedSource(compilation, "TestRequestExtensions.kt")

        assertContains(extensions, "fun TestRequest.Factory.from(ctx: RoutingContext)")
        assertContains(extensions, "fun TestRequest.Factory.route(router: Router")
        assertTrue(!extensions.contains("TestRequest.Companion"))
    }

    // --- No-content ---

    @Test
    fun `should skip body for 204 status code and warn`() {
        val src = SourceFile.kotlin("TestRequest.kt", """
            package com.example

            import io.github.chloeeekim.kontract.annotation.*

            data class EmptyResponse(val ok: Boolean)

            @VertxEndpoint(method = HttpMethod.DELETE, path = "/users/:id", response = EmptyResponse::class, statusCode = 204)
            data class DeleteUserRequest(
                @PathParam val id: Long,
            )
        """)

        val (result, compilation) = compileWithResult(src)
        val generated = findGeneratedSource(compilation, "DeleteUserRequestContract.kt")

        // body 직렬화 없이 종료
        assertContains(generated, ".setStatusCode(204).end()")
        assertTrue(!generated.contains("writeValueAsString"))
        // 경고 메시지
        assertContains(result.messages, "statusCode 204 on DeleteUserRequest does not allow a response body")
    }

    // --- Validation ---

    @Test
    fun `should generate validation code matching design doc example`() {
        val src = SourceFile.kotlin("TestRequest.kt", """
            package com.example

            import io.github.chloeeekim.kontract.annotation.*

            @VertxEndpoint(method = HttpMethod.GET, path = "/users/:userId")
            data class GetUserRequest(
                @PathParam @Min(1) val userId: Long,
                @QueryParam @Pattern("[a-zA-Z,]+") val fields: String? = null,
                @QueryParam @Size(min = 1, max = 100) val limit: Int = 20,
            )
        """)

        val generated = compileAndFindSource(src, "GetUserRequestContract.kt")

        // @Min(1)
        assertContains(generated, "if (userId < 1)")
        assertContains(generated, """"userId must be >= 1""")
        // @Pattern — precompiled Regex
        assertContains(generated, """private val fieldsPattern = Regex("[a-zA-Z,]+")""")
        assertContains(generated, """if (fields != null && !fields.matches(fieldsPattern))""")
        // @Size
        assertContains(generated, "if (limit < 1 || limit > 100)")
        assertContains(generated, """"limit must be between 1 and 100""")
    }

    @Test
    fun `should generate @NotBlank and @Max validation`() {
        val generated = compileAndFindSource(
            source("TestRequest", """
                @QueryParam @NotBlank val name: String,
                @QueryParam @Max(1000) val limit: Long,
            """),
            "TestRequestContract.kt",
        )

        assertContains(generated, """if (name.isNullOrBlank())""")
        assertContains(generated, """"name must not be blank"""")
        assertContains(generated, """if (limit > 1000)""")
        assertContains(generated, """"limit must be <= 1000""")
    }

    @Test
    fun `should generate @Size validation for String using length`() {
        val generated = compileAndFindSource(
            source("TestRequest", """@QueryParam @Size(min = 1, max = 50) val name: String,"""),
            "TestRequestContract.kt",
        )

        assertContains(generated, "name.length < 1 || name.length > 50")
        assertContains(generated, """"name length must be between 1 and 50""")
    }

    @Test
    fun `should error when @Min is used on String`() {
        val src = SourceFile.kotlin("TestRequest.kt", """
            package com.example

            import io.github.chloeeekim.kontract.annotation.*

            @VertxEndpoint(method = HttpMethod.GET, path = "/test")
            data class TestRequest(
                @QueryParam @Min(1) val name: String,
            )
        """)

        val (result, _) = compileWithResult(src)

        assertContains(result.messages, "@Min is only valid for Int or Long types, but 'name' is String")
    }

    @Test
    fun `should error when @Pattern is used on Long`() {
        val src = SourceFile.kotlin("TestRequest.kt", """
            package com.example

            import io.github.chloeeekim.kontract.annotation.*

            @VertxEndpoint(method = HttpMethod.GET, path = "/test")
            data class TestRequest(
                @QueryParam @Pattern("[0-9]+") val id: Long,
            )
        """)

        val (result, _) = compileWithResult(src)

        assertContains(result.messages, "@Pattern is only valid for String types, but 'id' is Long")
    }

    // --- Serialization ---

    @Test
    fun `should generate kotlinx serialization when KSP option is set`() {
        val src = SourceFile.kotlin("TestRequest.kt", """
            package com.example

            import io.github.chloeeekim.kontract.annotation.*

            data class Payload(val data: String)

            @VertxEndpoint(method = HttpMethod.POST, path = "/test")
            data class TestRequest(
                @BodyParam val body: Payload,
            )
        """)

        val compilation = KotlinCompilation().apply {
            sources = listOf(src)
            inheritClassPath = true
            configureKsp(useKsp2 = true) {
                symbolProcessorProviders += VertxContractProcessorProvider()
            }
            kspProcessorOptions = mutableMapOf("kontract.serializer" to "kotlinx")
        }
        compilation.compile()
        val generated = findGeneratedSource(compilation, "TestRequestContract.kt")

        assertContains(generated, "Json.decodeFromString<Payload>(ctx.body().asString())")
        assertContains(generated, "import kotlinx.serialization.json.Json")
        assertTrue(!generated.contains("objectMapper"))
    }

    @Test
    fun `should warn when @Default is used on @BodyParam`() {
        val src = SourceFile.kotlin("TestRequest.kt", """
            package com.example

            import io.github.chloeeekim.kontract.annotation.*

            data class Payload(val data: String)

            @VertxEndpoint(method = HttpMethod.POST, path = "/test")
            data class TestRequest(
                @BodyParam @Default("{}") val body: Payload,
            )
        """)

        val (result, _) = compileWithResult(src)

        assertContains(result.messages, "@Default on @BodyParam 'body' is ignored")
    }

    // --- helpers ---

    private fun source(className: String, params: String, path: String = "/test"): SourceFile {
        return SourceFile.kotlin("$className.kt", """
            package com.example

            import io.github.chloeeekim.kontract.annotation.*

            @VertxEndpoint(method = HttpMethod.GET, path = "$path")
            data class $className(
                $params
            )
        """)
    }

    private fun compileAndFindSource(source: SourceFile, fileName: String): String {
        val (_, compilation) = compileWithResult(source)
        return findGeneratedSource(compilation, fileName)
    }

    private fun compileWithResult(vararg sources: SourceFile): Pair<JvmCompilationResult, KotlinCompilation> {
        val compilation = KotlinCompilation().apply {
            this.sources = sources.toList()
            inheritClassPath = true
            configureKsp(useKsp2 = true) {
                symbolProcessorProviders += VertxContractProcessorProvider()
            }
        }
        val result = compilation.compile()
        return Pair(result, compilation)
    }

    private fun findGeneratedSource(compilation: KotlinCompilation, fileName: String): String {
        val generatedFiles = compilation.kspSourcesDir
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()
        val file = generatedFiles.firstOrNull { it.name == fileName }
        assertTrue(
            file != null,
            "Expected generated file '$fileName' not found. Generated: ${generatedFiles.map { it.name }}"
        )
        return file.readText()
    }
}
