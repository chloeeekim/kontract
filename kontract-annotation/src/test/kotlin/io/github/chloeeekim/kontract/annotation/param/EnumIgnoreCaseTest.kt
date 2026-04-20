package io.github.chloeeekim.kontract.annotation.param

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class EnumIgnoreCaseTest {

    data class SampleRequest(
        @EnumIgnoreCase val status: String,
    )

    @Test
    fun `should be present on annotated parameter`() {
        val param = SampleRequest::class.constructors.first()
            .parameters.first { it.name == "status" }
        val annotation = param.annotations.filterIsInstance<EnumIgnoreCase>()

        assertTrue(annotation.isNotEmpty())
    }
}
