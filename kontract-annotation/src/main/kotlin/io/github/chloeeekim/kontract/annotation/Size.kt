package io.github.chloeeekim.kontract.annotation

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Size(val min: Int = 0, val max: Int = Int.MAX_VALUE)
