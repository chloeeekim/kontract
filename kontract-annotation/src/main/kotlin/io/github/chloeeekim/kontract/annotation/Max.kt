package io.github.chloeeekim.kontract.annotation

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Max(val value: Long)
