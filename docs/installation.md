# Installation

## Gradle Plugin (Recommended)

```kotlin
// build.gradle.kts
plugins {
    id("com.google.devtools.ksp") version "2.0.0-1.0.24"
    id("io.github.chloeeekim.kontract") version "0.1.0"
}

// That's it — annotation, processor, and Jackson dependencies are added automatically

// To use kotlinx.serialization instead:
kontract {
    serializer.set("kotlinx")
}
```

## Manual Setup

```kotlin
// build.gradle.kts
plugins {
    id("com.google.devtools.ksp") version "2.0.0-1.0.24"
}

dependencies {
    implementation("io.vertx:vertx-core:4.5.10")
    implementation("io.vertx:vertx-web:4.5.10")

    implementation("io.github.chloeeekim:kontract-annotation:0.1.0-SNAPSHOT")
    ksp("io.github.chloeeekim:kontract-processor:0.1.0-SNAPSHOT")

    // For @BodyParam (Jackson mode, default)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
}
```
