package io.github.chloeeekim.kontract.annotation.param

/**
 * Specifies the default value of a parameter.
 * In KSP, the default value expression of a data class cannot be read directly,
 * so this annotation is used to define the default value as a string.
 *
 * Example:
 * ```
 * @QueryParam @Default("20") val limit: Int = 20
 * @QueryParam @Default("SortOrder.ASC") val sort: SortOrder = SortOrder.ASC
 * ```
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Default(val value: String)