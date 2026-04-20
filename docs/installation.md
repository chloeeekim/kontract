# Installation

Kontract is a **KSP**-based code generator: you declare request models with annotations, and Gradle runs the processor during compilation to emit `*Contract` classes. This page covers how to wire that into a **Gradle + Kotlin** project that already uses (or will use) **Vert.x Web** for HTTP routing.

You need three ideas in place:

1. **KSP** on the module that contains your endpoint request types (the KSP plugin version must match your Kotlin toolchain; see the [KSP releases](https://github.com/google/ksp/releases) matrix).
2. **Vert.x** on `implementation` (`vertx-core`, `vertx-web`, and anything else your app uses). The Kontract Gradle plugin adds Kontract artifacts and a body JSON library; it does **not** add Vert.x for you.
3. A **body serialization** choice for `@BodyParam`: **Jackson** (default) or **kotlinx.serialization**, aligned with how you want to decode JSON bodies.

After dependencies resolve, run a compile (for example `./gradlew kspKotlin` or `./gradlew build`) so generated sources exist before you reference `…Contract` or companion extensions. See [`quick-start.md`](quick-start.md).

## Gradle plugin (recommended)

The `io.github.chloeeekim.kontract` plugin registers the annotation JAR and the KSP processor, wires **KSP processor options**, adds the default **Jackson Kotlin** module (or swaps in **kotlinx-serialization-json** when configured), and tries to register the KSP output directory on the main Kotlin source set so the IDE sees generated code.

Apply **both** the KSP plugin and the Kontract plugin in the same module where your request data classes live. Order in the `plugins { }` block does not matter.

```kotlin
// build.gradle.kts
plugins {
    id("com.google.devtools.ksp") version "2.0.0-1.0.24"
    id("io.github.chloeeekim.kontract") version "0.2.0"
}

dependencies {
    implementation("io.vertx:vertx-core:4.5.10")
    implementation("io.vertx:vertx-web:4.5.10")
}
```

### `kontract { }` options

Optional DSL on the project:

```kotlin
kontract {
    // Body JSON for @BodyParam: "jackson" (default) or "kotlinx"
    serializer.set("kotlinx")

    // When true: adds vertx-lang-kotlin-coroutines and generates coRoute / coRouteWithResponse APIs
    coroutines.set(true)
}
```

If you choose `kotlinx`, apply Kotlin’s serialization plugin on the project and annotate your body types as usual for kotlinx.serialization; Kontract’s processor reads `kontract.serializer` and emits matching decode calls.

## Manual setup

Use manual dependencies when you want **full control** over versions (corporate mirrors, BOM alignment, or composite builds), or when you cannot use the Kontract Gradle plugin.

You still apply **KSP** yourself, then declare:

| Dependency | Configuration | Role |
|---|---|---|
| `kontract-annotation` | `implementation` | Annotations you use on request types (`@VertxEndpoint`, `@PathParam`, …) |
| `kontract-processor` | `ksp` | Code generator invoked by KSP |
| `jackson-module-kotlin` **or** `kotlinx-serialization-json` | `implementation` | JSON decoding for `@BodyParam` (must match processor option) |
| Vert.x (e.g. `vertx-core`, `vertx-web`) | `implementation` | Runtime APIs used by generated contracts |

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

For **kotlinx.serialization** instead of Jackson, depend on `kotlinx-serialization-json` (and your project’s serialization plugin), remove Jackson if you do not need it, and pass the option KSP expects:

```kotlin
ksp {
    arg("kontract.serializer", "kotlinx")
}
```

For **coroutine route helpers**, add `io.vertx:vertx-lang-kotlin-coroutines` and set `arg("kontract.coroutines", "true")` in the `ksp { }` block.

If the IDE does not index generated Kotlin under `build/generated/ksp/…`, add that directory to the relevant Kotlin source set for your layout (JVM, Android, or multiplatform differ). See the [KSP Gradle quickstart](https://kotlinlang.org/docs/ksp-quickstart.html).

