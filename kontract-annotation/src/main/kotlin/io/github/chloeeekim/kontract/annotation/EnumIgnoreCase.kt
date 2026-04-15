package io.github.chloeeekim.kontract.annotation

/**
 * When applied to an enum parameter, matching is case-insensitive.
 * Uses entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } instead of valueOf().
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class EnumIgnoreCase