package io.github.chloeeekim.kontract.gradle

import org.gradle.api.provider.Property

interface KontractExtension {
    /**
     * Configure for Body serialization library.
     * "jackson" (default) or "kotlinx".
     */
    val serializer: Property<String>

    /**
     * Whether to generate coRoute() / coRouteWithResponse() based on coroutineHandler.
     * If set to true, additional methods using suspend handlers are generated.
     */
    val coroutines: Property<Boolean>
}
