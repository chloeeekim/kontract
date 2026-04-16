package io.github.chloeeekim.kontract.annotation

import io.vertx.ext.web.RoutingContext

/**
 * Error handler invoked when parsing or validation fails in Kontract’s route() / routeWithResponse().
 *
 * The default implementation returns 400 status code with the exception message.
 * If custom error response format is needed, implement this interface and register it in [KontractConfig.errorHandler].
 */
fun interface KontractErrorHandler {
    fun handleError(ctx: RoutingContext, error: BadRequestException)
}
