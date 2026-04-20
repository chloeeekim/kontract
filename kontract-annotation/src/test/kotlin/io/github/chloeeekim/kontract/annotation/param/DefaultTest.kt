package io.github.chloeeekim.kontract.annotation.param

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DefaultTest {

    data class SampleRequest(
        @Default("20") val limit: Int = 20,
        @Default("SortOrder.ASC") val sort: String = "SortOrder.ASC",
    )

    @Test
    fun `should have correct default value for Int param`() {
        val param = SampleRequest::class.constructors.first()
            .parameters.first { it.name == "limit" }
        val annotation = param.annotations.filterIsInstance<Default>().first()

        assertEquals("20", annotation.value)
    }

    @Test
    fun `should have correct default value for String param`() {
        val param = SampleRequest::class.constructors.first()
            .parameters.first { it.name == "sort" }
        val annotation = param.annotations.filterIsInstance<Default>().first()

        assertEquals("SortOrder.ASC", annotation.value)
    }
}