tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
    }
}

dependencies {
    implementation(project(":kontract-annotation"))
    implementation(libs.ksp.symbol.processing.api)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kctfork.core)
    testImplementation(libs.kctfork.ksp)
}
