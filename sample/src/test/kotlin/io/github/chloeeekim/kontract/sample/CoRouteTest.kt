package io.github.chloeeekim.kontract.sample

import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(VertxExtension::class)
class CoRouteTest {

    private lateinit var router: Router

    @BeforeEach
    fun setup(vertx: Vertx) {
        router = Router.router(vertx)
    }

    @Test
    fun `coRoute should handle valid request with suspend handler`(vertx: Vertx, testContext: VertxTestContext) {
        GetItemRequest.coRoute(CoroutineScope(vertx.dispatcher()), router) { req, ctx ->
            delay(1) // suspend 호출 검증
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
    fun `coRoute should return error on invalid param`(vertx: Vertx, testContext: VertxTestContext) {
        GetItemRequest.coRoute(CoroutineScope(vertx.dispatcher()), router) { _, ctx ->
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
    fun `coRouteWithResponse should auto-serialize response`(vertx: Vertx, testContext: VertxTestContext) {
        GetItemRequest.coRouteWithResponse(CoroutineScope(vertx.dispatcher()), router) { req, _ ->
            delay(1)
            ItemResponse(id = req.id, name = "Item-${req.id}", price = 99.99)
        }

        startServerAndRequest(vertx, testContext, "/items/456") { statusCode, body ->
            testContext.verify {
                assertEquals(200, statusCode)
                assertTrue(body.contains("\"id\":456"))
                assertTrue(body.contains("\"name\":\"Item-456\""))
            }
            testContext.completeNow()
        }
    }

    @Test
    fun `coRoute should return 500 on uncaught exception in handler`(vertx: Vertx, testContext: VertxTestContext) {
        GetItemRequest.coRoute(CoroutineScope(vertx.dispatcher()), router) { _, _ ->
            throw RuntimeException("unexpected error")
        }

        startServerAndRequest(vertx, testContext, "/items/123") { statusCode, _ ->
            testContext.verify {
                assertEquals(500, statusCode)
            }
            testContext.completeNow()
        }
    }

    @Test
    fun `coRouteWithResponse should return 500 on uncaught exception`(vertx: Vertx, testContext: VertxTestContext) {
        GetItemRequest.coRouteWithResponse(CoroutineScope(vertx.dispatcher()), router) { _, _ ->
            throw RuntimeException("unexpected error")
        }

        startServerAndRequest(vertx, testContext, "/items/123") { statusCode, _ ->
            testContext.verify {
                assertEquals(500, statusCode)
            }
            testContext.completeNow()
        }
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
