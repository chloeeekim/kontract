package io.github.chloeeekim.kontract.annotation

/**
 * Customizes the error message thrown when a non-null parameter is missing a value.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Required(val message: String)
