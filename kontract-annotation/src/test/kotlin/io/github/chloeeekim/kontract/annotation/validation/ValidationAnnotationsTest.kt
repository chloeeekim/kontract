package io.github.chloeeekim.kontract.annotation.validation

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValidationAnnotationsTest {

    data class SampleRequest(
        @Min(1) val id: Long,
        @Max(100) val limit: Int,
        @NotBlank val name: String,
        @Size(min = 1, max = 50) val title: String,
        @Pattern("[a-zA-Z,]+") val fields: String,
    )

    @Test
    fun `should read @Min value`() {
        val param = SampleRequest::class.constructors.first()
            .parameters.first { it.name == "id" }
        val annotation = param.annotations.filterIsInstance<Min>().first()
        assertEquals(1L, annotation.value)
    }

    @Test
    fun `should read @Max value`() {
        val param = SampleRequest::class.constructors.first()
            .parameters.first { it.name == "limit" }
        val annotation = param.annotations.filterIsInstance<Max>().first()
        assertEquals(100L, annotation.value)
    }

    @Test
    fun `should detect @NotBlank`() {
        val param = SampleRequest::class.constructors.first()
            .parameters.first { it.name == "name" }
        assertTrue(param.annotations.filterIsInstance<NotBlank>().isNotEmpty())
    }

    @Test
    fun `should read @Size min and max`() {
        val param = SampleRequest::class.constructors.first()
            .parameters.first { it.name == "title" }
        val annotation = param.annotations.filterIsInstance<Size>().first()
        assertEquals(1, annotation.min)
        assertEquals(50, annotation.max)
    }

    @Test
    fun `should read @Pattern regex`() {
        val param = SampleRequest::class.constructors.first()
            .parameters.first { it.name == "fields" }
        val annotation = param.annotations.filterIsInstance<Pattern>().first()
        assertEquals("[a-zA-Z,]+", annotation.regex)
    }
}