package io.github.chloeeekim.kontract.annotation.param

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class BodyParamTest {

    data class Payload(val data: String)

    data class SampleRequest(
        @BodyParam val body: Payload,
    )

    @Test
    fun `should be present on annotated parameter`() {
        val param = SampleRequest::class.constructors.first()
            .parameters.first { it.name == "body" }
        val annotation = param.annotations.filterIsInstance<BodyParam>()

        assertTrue(annotation.isNotEmpty())
    }
}
