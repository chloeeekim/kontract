package io.github.chloeeekim.kontract.processor

sealed class ValidationInfo {
    data class Min(val value: Long) : ValidationInfo()
    data class Max(val value: Long) : ValidationInfo()
    data object NotBlank : ValidationInfo()
    data class Size(val min: Int, val max: Int) : ValidationInfo()
    data class Pattern(val regex: String) : ValidationInfo()
}