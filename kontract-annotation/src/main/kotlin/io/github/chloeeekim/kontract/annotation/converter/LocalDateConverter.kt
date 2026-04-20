package io.github.chloeeekim.kontract.annotation.converter

import java.time.LocalDate

class LocalDateConverter : ParamConverter<LocalDate> {
    override fun convert(value: String): LocalDate = LocalDate.parse(value)
}
