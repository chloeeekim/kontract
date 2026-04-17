# Quick Start

## 1. Define a request data class

```kotlin
@VertxEndpoint(method = HttpMethod.GET, path = "/users/:userId")
data class GetUserRequest(
    @PathParam val userId: Long,
    @QueryParam val fields: String? = null,
)
```

## 2. Use the generated Contract

Recommended for most projects: use companion extensions (see step 3).

```kotlin
// Option A: Parse manually
fun handleGetUser(ctx: RoutingContext) {
    val req = GetUserRequestContract.from(ctx)  // throws BadRequestException on invalid input
    ctx.json(userService.get(req.userId, req.fields))
}

// Option B: Auto-register route with error handling
GetUserRequestContract.route(router) { req, ctx ->
    ctx.json(userService.get(req.userId, req.fields))
}

// Option C: Auto-register route + auto-serialize response
@VertxEndpoint(method = HttpMethod.GET, path = "/users/:userId", response = UserResponse::class)
data class GetUserRequest(@PathParam val userId: Long)

GetUserRequestContract.routeWithResponse(router) { req, _ ->
    UserResponse(id = req.userId, name = "User-${req.userId}")
}
```

## 3. Companion extensions (recommended)

For better readability and discoverability, add `companion object` and call APIs directly on the request class:

```kotlin
@VertxEndpoint(method = HttpMethod.GET, path = "/users/:userId", response = UserResponse::class)
data class GetUserRequest(@PathParam val userId: Long) {
    companion object
}

// Now you can call directly:
GetUserRequest.route(router) { req, ctx -> ... }
GetUserRequest.routeWithResponse(router) { req, _ -> UserResponse(...) }
val req = GetUserRequest.from(ctx)
```
