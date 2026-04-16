package io.github.chloeeekim.kontract.annotation.converter

import io.github.chloeeekim.kontract.annotation.ParamConverter
import java.math.BigDecimal

class BigDecimalConverter : ParamConverter<BigDecimal> {
    override fun convert(value: String): BigDecimal = BigDecimal(value)
}
