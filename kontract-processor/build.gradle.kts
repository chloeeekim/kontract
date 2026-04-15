tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
    }
}

dependencies {
    implementation(project(":kontract-annotation"))
    implementation("com.google.devtools.ksp:symbol-processing-api:2.0.0-1.0.24")

    testImplementation("dev.zacsweers.kctfork:core:0.7.0")
    testImplementation("dev.zacsweers.kctfork:ksp:0.7.0")
}
