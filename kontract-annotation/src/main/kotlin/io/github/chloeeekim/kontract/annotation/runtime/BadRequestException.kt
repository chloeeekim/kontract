package io.github.chloeeekim.kontract.annotation.runtime

class BadRequestException(
    override val message: String,
    override val cause: Throwable? = null,
) : RuntimeException(message, cause)