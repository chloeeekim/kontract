package io.github.chloeeekim.kontract.annotation

/**
 * Global configuration for the Kontract library.
 */
object KontractConfig {

    @Volatile
    var errorHandler: KontractErrorHandler = KontractErrorHandler { ctx, error ->
        ctx.response()
            .setStatusCode(400)
            .putHeader("Content-Type", "text/plain")
            .end(error.message)
    }
}
