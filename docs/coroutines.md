# Coroutine Support

Enable coroutine-based handlers with the `coroutines` option.
This generates `coRoute()` and `coRouteWithResponse()` methods that use Vert.x `coroutineHandler` with `suspend` lambdas.

```kotlin
// Gradle plugin DSL
kontract {
    coroutines.set(true)
}

// Or manual KSP option
ksp {
    arg("kontract.coroutines", "true")
}
```

```kotlin
// In a CoroutineVerticle — scope is tied to Verticle lifecycle
GetUserRequest.coRoute(this, router) { req, ctx ->
    val user = userService.getAsync(req.userId)  // suspend call
    ctx.json(user)
}

GetUserRequest.coRouteWithResponse(this, router) { req, _ ->
    userService.getAsync(req.userId)  // suspend call, auto-serialized
}

// Standalone — create your own scope
val scope = CoroutineScope(vertx.dispatcher())
GetUserRequest.coRoute(scope, router) { req, ctx -> ... }
```

Requires `io.vertx:vertx-lang-kotlin-coroutines` dependency (auto-added by Gradle plugin).
The first parameter is a `CoroutineScope` — pass `this` from a `CoroutineVerticle` for automatic lifecycle management.
Companion extensions are also generated when `companion object` is declared.
