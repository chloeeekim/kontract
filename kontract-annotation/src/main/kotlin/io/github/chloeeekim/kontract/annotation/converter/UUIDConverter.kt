package io.github.chloeeekim.kontract.annotation.converter

import java.util.UUID

class UUIDConverter : ParamConverter<UUID> {
    override fun convert(value: String): UUID = UUID.fromString(value)
}
