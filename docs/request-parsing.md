# Request Parsing

## Parameter Sources

| Annotation | Source | Example                                                 |
|---|---|---------------------------------------------------------|
| `@PathParam` | URL path | `@PathParam val userId: Long`                           |
| `@QueryParam` | Query string | `@QueryParam val fields: String?`                       |
| `@HeaderParam` | HTTP header | `@HeaderParam("X-Forwarded-For") val clientIp: String?` |
| `@CookieParam` | Cookie | `@CookieParam("session_id") val sessionId: String?`     |
| `@BodyParam` | Request body (JSON) | `@BodyParam val body: CreatePayload`                    |

When `name` is empty, the property name is used as the parameter name.

## Supported Types

| Type | Parsing |
|---|---|
| `String` | Raw value |
| `Int`, `Long` | `toIntOrNull()` / `toLongOrNull()` |
| `Boolean` | `toBooleanStrictOrNull()` — only `"true"` / `"false"` accepted |
| `Enum` | `valueOf()`, with optional `@EnumIgnoreCase` |
| `List<T>` | Multi-value query param (`@QueryParam` only, see below) |
| Custom types | Via `@TypeConverter` (see related doc) |
| Body objects | Jackson or kotlinx.serialization |

## Multi-value Query Parameters

Use `List<T>` to receive multiple values for the same query parameter (for example, `/users?id=1&id=2&id=3`).

```kotlin
@VertxEndpoint(method = HttpMethod.GET, path = "/users")
data class ListUserRequest(
    @QueryParam val ids: List<Long>,            // required — empty list throws 400
    @QueryParam val tags: List<String>? = null, // optional — null if absent
)
```

Supported element types: `String`, `Int`, `Long`, `Boolean`, `Enum`.
`List<T>` is only supported for `@QueryParam`.
`@Default` and `@TypeConverter` cannot be used with list parameters.

## Required / Optional / Default

| Declaration | When parameter is absent |
|---|---|
| `val id: Long` | `BadRequestException` (required) |
| `val id: Long?` | `null` |
| `@Default("20") val limit: Int` | `20` |

Invalid values (for example, `?limit=abc`) always throw `BadRequestException`, regardless of nullability.
