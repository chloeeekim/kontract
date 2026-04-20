package io.github.chloeeekim.kontract.annotation.converter

import java.time.LocalDateTime

class LocalDateTimeConverter : ParamConverter<LocalDateTime> {
    override fun convert(value: String): LocalDateTime = LocalDateTime.parse(value)
}
