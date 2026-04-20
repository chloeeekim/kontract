package io.github.chloeeekim.kontract.annotation.validation

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Min(val value: Long)
