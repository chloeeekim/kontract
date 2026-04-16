package io.github.chloeeekim.kontract.annotation

import kotlin.reflect.KClass

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class TypeConverter(val converter: KClass<out ParamConverter<*>>)
