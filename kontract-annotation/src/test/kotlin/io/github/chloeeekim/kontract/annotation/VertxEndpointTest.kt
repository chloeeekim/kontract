package io.github.chloeeekim.kontract.annotation

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class VertxEndpointTest {

    @VertxEndpoint(method = HttpMethod.GET, path = "/users/:userId")
    data class SampleGetRequest(val userId: Long)

    @VertxEndpoint(method = HttpMethod.POST, path = "/users")
    data class SamplePostRequest(val name: String)

    @Test
    fun `should have correct method and path on GET endpoint`() {
        val annotation = SampleGetRequest::class.annotations
            .filterIsInstance<VertxEndpoint>()
            .first()

        assertEquals(HttpMethod.GET, annotation.method)
        assertEquals("/users/:userId", annotation.path)
    }

    @Test
    fun `should have correct method and path on POST endpoint`() {
        val annotation = SamplePostRequest::class.annotations
            .filterIsInstance<VertxEndpoint>()
            .first()

        assertEquals(HttpMethod.POST, annotation.method)
        assertEquals("/users", annotation.path)
    }
}
