package io.github.chloeeekim.kontract.annotation.param

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class HeaderParam(val name: String = "")
