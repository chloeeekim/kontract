package io.github.chloeeekim.kontract.annotation

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PathParamTest {

    data class SampleRequest(
        @PathParam val id: Long,
        @PathParam(name = "userId") val user: Long,
    )

    @Test
    fun `should default name to empty string when not specified`() {
        val param = SampleRequest::class.constructors.first()
            .parameters.first { it.name == "id" }
        val annotation = param.annotations.filterIsInstance<PathParam>().first()

        assertEquals("", annotation.name)
    }

    @Test
    fun `should use specified name when provided`() {
        val param = SampleRequest::class.constructors.first()
            .parameters.first { it.name == "user" }
        val annotation = param.annotations.filterIsInstance<PathParam>().first()

        assertEquals("userId", annotation.name)
    }
}
