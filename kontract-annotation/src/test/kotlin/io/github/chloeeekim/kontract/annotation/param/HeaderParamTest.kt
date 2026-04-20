package io.github.chloeeekim.kontract.annotation.param

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HeaderParamTest {

    data class SampleRequest(
        @HeaderParam val contentType: String,
        @HeaderParam(name = "X-Forwarded-For") val clientIp: String,
    )

    @Test
    fun `should default name to empty string`() {
        val param = SampleRequest::class.constructors.first()
            .parameters.first { it.name == "contentType" }
        val annotation = param.annotations.filterIsInstance<HeaderParam>().first()

        assertEquals("", annotation.name)
    }

    @Test
    fun `should use specified name`() {
        val param = SampleRequest::class.constructors.first()
            .parameters.first { it.name == "clientIp" }
        val annotation = param.annotations.filterIsInstance<HeaderParam>().first()

        assertEquals("X-Forwarded-For", annotation.name)
    }
}
