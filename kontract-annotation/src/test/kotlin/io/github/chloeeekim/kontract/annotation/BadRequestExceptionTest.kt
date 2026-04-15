package io.github.chloeeekim.kontract.annotation

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BadRequestExceptionTest {

    @Test
    fun `should create exception with message`() {
        val exception = BadRequestException("Missing required param: deviceId")

        assertEquals("Missing required param: deviceId", exception.message)
        assertNull(exception.cause)
    }

    @Test
    fun `should create exception with message and cause`() {
        val cause = NumberFormatException("For input string: \"abc\"")
        val exception = BadRequestException("Invalid path param: deviceId", cause)

        assertEquals("Invalid path param: deviceId", exception.message)
        assertSame(cause, exception.cause)
    }

    @Test
    fun `should be a RuntimeException`() {
        val exception: Any = BadRequestException("test")

        assertTrue(exception is RuntimeException)
    }

    @Test
    fun `should be throwable and catchable`() {
        val thrown = assertThrows<BadRequestException> {
            throw BadRequestException("Missing query param: fields")
        }

        assertEquals("Missing query param: fields", thrown.message)
    }
}
