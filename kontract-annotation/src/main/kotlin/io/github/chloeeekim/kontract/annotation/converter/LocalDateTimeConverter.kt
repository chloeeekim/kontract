package io.github.chloeeekim.kontract.annotation.converter

import io.github.chloeeekim.kontract.annotation.ParamConverter
import java.time.LocalDateTime

class LocalDateTimeConverter : ParamConverter<LocalDateTime> {
    override fun convert(value: String): LocalDateTime = LocalDateTime.parse(value)
}
