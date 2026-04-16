package io.github.chloeeekim.kontract.gradle

import org.gradle.api.provider.Property

interface KontractExtension {
    /**
     * Configure for Body serialization library.
     * "jackson" (default) or "kotlinx".
     */
    val serializer: Property<String>
}
