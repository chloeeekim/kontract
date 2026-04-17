package io.github.chloeeekim.kontract.sample

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler

fun main() {
    val vertx = Vertx.vertx()
    val router = Router.router(vertx)

    // When using @BodyParam, registering a BodyHandler is required.
    router.route().handler(BodyHandler.create())

    // --- Method 1: Call directly via a companion extension (recommended; requires a companion object) ---

    // GET /items/:id - specifying a response type example
    GetItemRequest.routeWithResponse(router) { req, _ ->
        ItemResponse(id = req.id, name = "Keyboard", price = 129.99)
    }

    // GET /items - Enum, Default, Validation example
    ListItemRequest.route(router) { req, ctx ->
        ctx.json(JsonObject()
            .put("type", req.type.name)
            .put("sort", req.sort.name)
            .put("page", req.page)
            .put("limit", req.limit)
            .put("fields", req.fields)
        )
    }

    // POST /items - Body, Header, Cookie example
    CreateItemRequest.route(router) { req, ctx ->
        ctx.json(JsonObject()
            .put("name", req.body.name)
            .put("type", req.body.type.name)
            .put("requestId", req.requestId)
            .put("sessionId", req.sessionId)
        )
    }

    // --- Method 2: Call via the Contract object (no companion object required) ---

    // GET /items/search - Required, List param, Boolean example
    SearchItemsRequestContract.route(router) { req, ctx ->
        ctx.json(JsonObject()
            .put("keyword", req.keyword)
            .put("ids", req.ids)
            .put("active", req.active)
        )
    }

    // GET /events - TypeConverter example
    GetEventsRequestContract.route(router) { req, ctx ->
        ctx.json(JsonObject()
            .put("id", req.eventId.toString())
            .put("startDate", req.startDate?.toString() ?: "N/A")
            .put("endDate", req.endDate?.toString() ?: "N/A")
        )
    }

    // --- Coroutines Example ---

    // When the coroutines option is enabled, coRoute() / coRouteWithResponse() can be used.
    //
    // In a CoroutineVerticle — scope is tied to Verticle lifecycle:
    // GetItemRequest.coRouteWithResponse(this, router) { req, _ ->
    //     val item = itemService.getAsync(req.id)  // suspend
    //     ItemResponse(id = item.id, name = item.name, price = item.price)
    // }
    //
    // Standalone:
    // val scope = CoroutineScope(vertx.dispatcher())
    // GetItemRequest.coRoute(scope, router) { req, ctx ->
    //     val item = itemService.getAsync(req.id)
    //     ctx.json(item)
    // }

    vertx.createHttpServer()
        .requestHandler(router)
        .listen(8080)
        .onSuccess { println("Server started on port ${it.actualPort()}") }
        .onFailure { println("Failed to start: ${it.message}") }
}