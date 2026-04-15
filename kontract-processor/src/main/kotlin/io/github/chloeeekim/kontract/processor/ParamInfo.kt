package io.github.chloeeekim.kontract.processor

data class ParamInfo(
    val name: String,
    val typeName: String,
    val qualifiedTypeName: String = typeName,
    val source: ParamSource,
    val nullable: Boolean,
    val annotationName: String,
    val defaultValue: String? = null,
    val isEnum: Boolean = false,
    val enumIgnoreCase: Boolean = false,
)
