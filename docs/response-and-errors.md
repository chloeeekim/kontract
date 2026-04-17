# Response and Errors

## Error Handling

By default, parsing/validation errors return HTTP 400 with a plain text message.
Customize the global error response:

```kotlin
KontractConfig.errorHandler = KontractErrorHandler { ctx, error ->
    ctx.response()
        .setStatusCode(400)
        .putHeader("Content-Type", "application/json")
        .end("""{"error": "${error.message}", "code": "BAD_REQUEST"}""")
}
```

## Response Serialization

Specify a `response` type to auto-serialize the handler return value:

```kotlin
@VertxEndpoint(
    method = HttpMethod.POST,
    path = "/users",
    response = UserResponse::class,
    statusCode = 201,
)
data class CreateUserRequest(@BodyParam val body: CreateUserPayload) {
    companion object
}

CreateUserRequest.routeWithResponse(router) { req, _ ->
    userService.create(req.body)  // return value is auto-serialized to JSON
}
```

Status codes 204, 205, and 304 automatically omit the response body.

## Body Serialization Mode

```kotlin
// build.gradle.kts — or use the Gradle plugin DSL
ksp {
    arg("kontract.serializer", "jackson")   // default
    // arg("kontract.serializer", "kotlinx")
}
```

| Mode | Generated code | Required dependency |
|---|---|---|
| `jackson` | `objectMapper.readValue(...)` | jackson-module-kotlin |
| `kotlinx` | `Json.decodeFromString<T>(...)` | kotlinx-serialization-json |
