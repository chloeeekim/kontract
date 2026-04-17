package io.github.chloeeekim.kontract.sample

import io.github.chloeeekim.kontract.annotation.*
import io.github.chloeeekim.kontract.annotation.converter.LocalDateConverter
import io.github.chloeeekim.kontract.annotation.converter.UUIDConverter
import java.time.LocalDate
import java.util.UUID

/**
 * GET /items/:id - specifying a response type example
 */
@VertxEndpoint(method = HttpMethod.GET, path = "/items/:id", response = ItemResponse::class)
data class GetItemRequest(
    @PathParam val id: Long
) {
    companion object
}

data class ItemResponse(
    val id: Long,
    val name: String,
    val price: Double,
)

/**
 * GET /items - Enum, Default, Validation example
 */
@VertxEndpoint(method = HttpMethod.GET, path = "/items")
data class ListItemRequest(
    @QueryParam @EnumIgnoreCase val type: ItemType,
    @QueryParam @Default("SortOrder.ASC") val sort: SortOrder = SortOrder.ASC,
    @QueryParam @Default("1") @Min(1) val page: Int = 1,
    @QueryParam @Default("20") @Size(min = 1, max = 100) val limit: Int = 20,
    @QueryParam @Pattern("[a-zA-Z,]*") val fields: String? = null,
) {
    companion object
}

enum class ItemType { Fashion, Beauty, Electronics }
enum class SortOrder { ASC, DESC }

/**
 * POST /items - Body, Header, Cookie example
 */
@VertxEndpoint(method = HttpMethod.POST, path = "/items")
data class CreateItemRequest(
    @HeaderParam("X-Request-ID") val requestId: String? = null,
    @CookieParam("session_id") val sessionId: String? = null,
    @BodyParam val body: CreateItemPayload
) {
    companion object
}

data class CreateItemPayload(
    val name: String,
    val type: ItemType,
)

/**
 * GET /items/search - Required, List param, Boolean example
 */
@VertxEndpoint(method = HttpMethod.GET, path = "/items/search")
data class SearchItemsRequest(
    @QueryParam @Required("Please enter a keyword.") val keyword: String,
    @QueryParam val ids: List<Long>? = null,
    @QueryParam @Default("true") val active: Boolean = true,
) {
    companion object
}

/**
 * GET /events - TypeConverter example
 */
@VertxEndpoint(method = HttpMethod.GET, path = "/events/:eventId")
data class GetEventsRequest(
    @PathParam @TypeConverter(UUIDConverter::class) val eventId: UUID,
    @QueryParam @TypeConverter(LocalDateConverter::class) val startDate: LocalDate? = null,
    @QueryParam @TypeConverter(LocalDateConverter::class) val endDate: LocalDate? = null,
) {
    companion object
}