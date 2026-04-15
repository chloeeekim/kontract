package io.github.chloeeekim.kontract.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class VertxEndpoint(
    val method: HttpMethod,
    val path: String,
)
