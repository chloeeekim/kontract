package io.github.chloeeekim.kontract.annotation.param

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class QueryParamTest {

    data class SampleRequest(
        @QueryParam val fields: String,
        @QueryParam(name = "field_names") val fieldNames: String,
    )

    @Test
    fun `should default name to empty string when not specified`() {
        val param = SampleRequest::class.constructors.first()
            .parameters.first { it.name == "fields" }
        val annotation = param.annotations.filterIsInstance<QueryParam>().first()

        assertEquals("", annotation.name)
    }

    @Test
    fun `should use specified name when provided`() {
        val param = SampleRequest::class.constructors.first()
            .parameters.first { it.name == "fieldNames" }
        val annotation = param.annotations.filterIsInstance<QueryParam>().first()

        assertEquals("field_names", annotation.name)
    }
}
