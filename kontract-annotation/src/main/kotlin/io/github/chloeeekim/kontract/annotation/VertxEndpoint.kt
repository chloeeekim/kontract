package io.github.chloeeekim.kontract.annotation

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class VertxEndpoint(
    val method: HttpMethod,
    val path: String,
    val response: KClass<*> = Nothing::class,
    val statusCode: Int = 200,
)
