# Kontract

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-blue.svg)](https://kotlinlang.org)
[![Vert.x](https://img.shields.io/badge/Vert.x-4.5+-purple.svg)](https://vertx.io)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

### Stop parsing requests manually in Vert.x.

**Type-safe request parsing, validation, and error handling — generated at compile time with zero reflection.**

Kontract turns annotated request data classes into compile-time generated Vert.x contracts.
No reflection, less handler boilerplate, and safer refactoring.

- Compile-time generated parsing and validation
- Consistent bad-request responses across endpoints
- Clean handlers focused on business logic

👉 Full working example: [`sample`](sample/)

## Why Kontract?

❌ When using plain Vert.x, parsing and validating parameters is usually repeated in each handler:

```kotlin
router.get("/users/:userId").handler { ctx ->
    val userIdRaw = ctx.pathParam("userId")
        ?: return@handler ctx.response().setStatusCode(400).end("Missing path param: userId")
    val userId = userIdRaw.toLongOrNull()
        ?: return@handler ctx.response().setStatusCode(400).end("userId must be Long")
    if (userId < 1) {
        return@handler ctx.response().setStatusCode(400).end("userId must be >= 1")
    }

    val fields = ctx.queryParam("fields").firstOrNull()
    val user = userService.get(userId, fields)
    ctx.json(user)
}
```

✅ With Kontract, the same intent is defined once in a request model and generated contract:

```kotlin
@VertxEndpoint(method = HttpMethod.GET, path = "/users/:userId")
data class GetUserRequest(
    @PathParam @Min(1) val userId: Long,
    @QueryParam val fields: String? = null,
)

GetUserRequest.route(router) { req, ctx ->
    val user = userService.get(req.userId, req.fields)
    ctx.json(user)
}
```

- Less boilerplate in route handlers
- Validation rules stay close to request definitions
- Consistent HTTP 400 behavior for invalid inputs

## Features

- **Compile-time code generation** via KSP — no runtime reflection overhead
- **Type-safe request parsing** for path, query, header, cookie, and body parameters
- **Built-in validation and consistent error handling**
- **Extensible type conversion**
- **Flexible serialization and responses** — serialization with optional auto-response handling
- **Coroutine support** — generated coroutine route APIs
- **Gradle plugin for zero-config setup**

## Installation

Recommended setup (use the Gradle plugin for zero-config setup):

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.0.0-1.0.24"
    id("io.github.chloeeekim.kontract") version "0.2.0"
}
```

Manual setup is also available when you need fine-grained dependency control.

More options: [`docs/installation.md`](docs/installation.md)

## Quick Start

Get started in 3 steps: define request, generate contract, register route.

After you define or change the request data class, run Gradle so KSP generates the `*Contract` type (for example `./gradlew kspKotlin` or `./gradlew build`). Until that step succeeds, companion extensions such as `GetUserRequest.route` are not available because the contract class does not exist yet.

```kotlin
@VertxEndpoint(method = HttpMethod.GET, path = "/users/:userId")
data class GetUserRequest(
    @PathParam val userId: Long,
    @QueryParam val fields: String? = null,
) {
    companion object
}

GetUserRequest.route(router) { req, ctx ->
    ctx.json(userService.get(req.userId, req.fields))
}
```

More examples: [`docs/quick-start.md`](docs/quick-start.md)

## Request Parsing and Validation

Kontract supports path/query/header/cookie/body parameters, required/optional/default rules,
built-in validations, custom required messages, and custom converters.

Details:
- [`docs/request-parsing.md`](docs/request-parsing.md)
- [`docs/validation-and-converters.md`](docs/validation-and-converters.md)

## Error Handling and Response

Configure global error handling and optional response auto-serialization.

Details: [`docs/response-and-errors.md`](docs/response-and-errors.md)

## Coroutine Support

Enable coroutine route generation with `coRoute()` and `coRouteWithResponse()`.

Details: [`docs/coroutines.md`](docs/coroutines.md)

## Project Structure

```
vertx-contract/
├── kontract-annotation/      ← Annotations + runtime support types
├── kontract-processor/       ← KSP processor (compile-time only)
├── gradle-plugin/            ← Gradle plugin for auto-configuration
└── sample/                   ← Usage examples and integration tests
```

## Requirements

- Kotlin 2.0+
- Vert.x Web 4.5+
- JDK 17+

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
