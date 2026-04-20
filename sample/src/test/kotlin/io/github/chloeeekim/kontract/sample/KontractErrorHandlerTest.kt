package io.github.chloeeekim.kontract.sample

import io.github.chloeeekim.kontract.annotation.runtime.KontractConfig
import io.github.chloeeekim.kontract.annotation.runtime.KontractErrorHandler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(VertxExtension::class)
class ContractErrorHandlerTest {

    private lateinit var router: Router

    private val defaultHandler = KontractConfig.errorHandler

    @BeforeEach
    fun setup(vertx: Vertx) {
        router = Router.router(vertx)
    }

    @AfterEach
    fun teardown() {
        KontractConfig.errorHandler = defaultHandler
    }

    @Test
    fun `default error handler should return 400 with plain text message`(vertx: Vertx, testContext: VertxTestContext) {
        GetItemRequest.route(router) { _, ctx ->
            ctx.response().end("ok")
        }

        startServerAndRequest(vertx, testContext, "/items/abc") { statusCode, body ->
            testContext.verify {
                assertEquals(400, statusCode)
                assertTrue(body.contains("Invalid value"))
            }
            testContext.completeNow()
        }
    }

    @Test
    fun `custom error handler should return JSON error response`(vertx: Vertx, testContext: VertxTestContext) {
        KontractConfig.errorHandler = KontractErrorHandler { ctx, error ->
            ctx.response()
                .setStatusCode(400)
                .putHeader("Content-Type", "application/json")
                .end(JsonObject().put("error", error.message).put("code", "BAD_REQUEST").encode())
        }

        GetItemRequest.route(router) { _, ctx ->
            ctx.response().end("ok")
        }

        startServerAndRequest(vertx, testContext, "/items/abc") { statusCode, body ->
            testContext.verify {
                assertEquals(400, statusCode)
                val json = JsonObject(body)
                assertEquals("BAD_REQUEST", json.getString("code"))
                assertTrue(json.getString("error").contains("Invalid value"))
            }
            testContext.completeNow()
        }
    }

    @Test
    fun `custom error handler should work with routeWithResponse`(vertx: Vertx, testContext: VertxTestContext) {
        KontractConfig.errorHandler = KontractErrorHandler { ctx, error ->
            ctx.response()
                .setStatusCode(422)
                .putHeader("Content-Type", "application/json")
                .end(JsonObject().put("message", error.message).encode())
        }

        GetItemRequest.routeWithResponse(router) { req, _ ->
            ItemResponse(id = req.id, name = "test", price = 129.99)
        }

        startServerAndRequest(vertx, testContext, "/items/abc") { statusCode, body ->
            testContext.verify {
                assertEquals(422, statusCode)
                val json = JsonObject(body)
                assertTrue(json.getString("message").contains("Invalid value"))
            }
            testContext.completeNow()
        }
    }

    @Test
    fun `custom error handler should not affect successful requests`(vertx: Vertx, testContext: VertxTestContext) {
        KontractConfig.errorHandler = KontractErrorHandler { ctx, error ->
            ctx.response()
                .setStatusCode(422)
                .end(JsonObject().put("error", error.message).encode())
        }

        GetItemRequest.route(router) { req, ctx ->
            ctx.response().end("id=${req.id}")
        }

        startServerAndRequest(vertx, testContext, "/items/123") { statusCode, body ->
            testContext.verify {
                assertEquals(200, statusCode)
                assertEquals("id=123", body)
            }
            testContext.completeNow()
        }
    }

    @Test
    fun `error handler change should take effect immediately`(vertx: Vertx, testContext: VertxTestContext) {
        ListItemRequest.route(router) { _, ctx ->
            ctx.response().end("ok")
        }

        vertx.createHttpServer().requestHandler(router).listen(0).onComplete(testContext.succeeding { server ->
            val port = server.actualPort()
            val client = WebClient.create(vertx)

            // Request handled by the default handler -> 400
            client.get(port, "localhost", "/items").send().onComplete(testContext.succeeding { firstResponse ->
                testContext.verify { assertEquals(400, firstResponse.statusCode()) }

                // Change the handler
                KontractConfig.errorHandler = KontractErrorHandler { ctx, _ ->
                    ctx.response().setStatusCode(503).end("custom")
                }

                // Request handled by the custom handler -> 503
                client.get(port, "localhost", "/items").send().onComplete(testContext.succeeding { secondResponse ->
                    testContext.verify {
                        assertEquals(503, secondResponse.statusCode())
                        assertEquals("custom", secondResponse.bodyAsString())
                    }
                    testContext.completeNow()
                })
            })
        })
    }

    private fun startServerAndRequest(
        vertx: Vertx,
        testContext: VertxTestContext,
        path: String,
        handler: (Int, String) -> Unit,
    ) {
        vertx.createHttpServer().requestHandler(router).listen(0).onComplete(testContext.succeeding { server ->
            val port = server.actualPort()
            WebClient.create(vertx).get(port, "localhost", path).send().onComplete(testContext.succeeding { response ->
                handler(response.statusCode(), response.bodyAsString() ?: "")
            })
        })
    }
}
