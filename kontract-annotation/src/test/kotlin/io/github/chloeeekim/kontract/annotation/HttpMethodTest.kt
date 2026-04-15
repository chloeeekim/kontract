package io.github.chloeeekim.kontract.annotation

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertContains

class HttpMethodTest {

    @Test
    fun `should define all standard HTTP methods`() {
        val methods = HttpMethod.entries.map { it.name }

        assertContains(methods, "GET")
        assertContains(methods, "POST")
        assertContains(methods, "PUT")
        assertContains(methods, "DELETE")
        assertContains(methods, "PATCH")
        assertContains(methods, "HEAD")
        assertContains(methods, "OPTIONS")
    }

    @Test
    fun `should resolve from string via valueOf`() {
        assertEquals(HttpMethod.GET, HttpMethod.valueOf("GET"))
        assertEquals(HttpMethod.POST, HttpMethod.valueOf("POST"))
    }
}
