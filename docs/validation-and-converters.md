# Validation and Converters

## Custom Error Messages (`@Required`)

By default, missing required parameters throw `BadRequestException` with a generic message such as `"Missing query param: keyword"`.
Use `@Required` to provide a custom, user-friendly error message:

```kotlin
@VertxEndpoint(method = HttpMethod.GET, path = "/users/search")
data class SearchUserRequest(
    @QueryParam @Required("Please enter a search keyword") val keyword: String,
    @PathParam @Required("User ID is required") val userId: Long,
)
```

When `keyword` is missing, the error message becomes `"Please enter a search keyword"`.

Notes:
- `@Required` only affects non-null parameters without `@Default`.
- On nullable or defaulted parameters, it is ignored (with a compile-time warning).
- `@Required` on `@BodyParam` is ignored. Body parameters use deserialization error messages.

## Validation

Validation code is generated at compile time and runs immediately after parsing.

| Annotation | Target types | Description |
|---|---|---|
| `@Min(value)` | Int, Long | Minimum value |
| `@Max(value)` | Int, Long | Maximum value |
| `@NotBlank` | String | Must not be blank |
| `@Size(min, max)` | Int, Long, String | Value range or string length |
| `@Pattern(regex)` | String | Regex matching |

```kotlin
@VertxEndpoint(method = HttpMethod.GET, path = "/items")
data class ListItemRequest(
    @QueryParam val type: ItemType,
    @QueryParam @Default("1") @Min(1) val page: Int = 1,
    @QueryParam @Default("20") @Size(min = 1, max = 100) val limit: Int = 20,
    @QueryParam @Pattern("[a-zA-Z,]*") val fields: String? = null,
)
```

## Custom Type Converters

For types beyond String/Int/Long/Enum, implement `ParamConverter<T>` and use `@TypeConverter`:

```kotlin
class LocalDateConverter : ParamConverter<LocalDate> {
    override fun convert(value: String): LocalDate = LocalDate.parse(value)
}

@VertxEndpoint(method = HttpMethod.GET, path = "/events/:eventId")
data class GetEventRequest(
    @PathParam @TypeConverter(UUIDConverter::class) val eventId: UUID,
    @QueryParam @TypeConverter(LocalDateConverter::class) val startDate: LocalDate? = null,
)
```

Built-in converters: `LocalDateConverter`, `LocalDateTimeConverter`, `UUIDConverter`, `BigDecimalConverter`
