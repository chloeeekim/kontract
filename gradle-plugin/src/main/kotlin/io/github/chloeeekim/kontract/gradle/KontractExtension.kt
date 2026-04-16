package io.github.chloeeekim.kontract.gradle

import org.gradle.api.provider.Property

interface KontractExtension {
    /**
     * Body 직렬화 라이브러리 설정.
     * "jackson" (기본값) 또는 "kotlinx".
     */
    val serializer: Property<String>
}
