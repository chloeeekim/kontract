package io.github.chloeeekim.kontract.annotation.param

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CookieParamTest {

    data class SampleRequest(
        @CookieParam val token: String,
        @CookieParam(name = "session_id") val sessionId: String,
    )

    @Test
    fun `should default name to empty string`() {
        val param = SampleRequest::class.constructors.first()
            .parameters.first { it.name == "token" }
        val annotation = param.annotations.filterIsInstance<CookieParam>().first()

        assertEquals("", annotation.name)
    }

    @Test
    fun `should use specified name`() {
        val param = SampleRequest::class.constructors.first()
            .parameters.first { it.name == "sessionId" }
        val annotation = param.annotations.filterIsInstance<CookieParam>().first()

        assertEquals("session_id", annotation.name)
    }
}
