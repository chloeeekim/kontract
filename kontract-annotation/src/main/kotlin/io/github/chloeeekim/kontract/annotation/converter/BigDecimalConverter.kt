package io.github.chloeeekim.kontract.annotation.converter

import java.math.BigDecimal

class BigDecimalConverter : ParamConverter<BigDecimal> {
    override fun convert(value: String): BigDecimal = BigDecimal(value)
}
