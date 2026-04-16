package io.github.chloeeekim.kontract.gradle

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Integration test to verify the configureWithKsp logic when applied together with the KSP plugin.
 *
 * Since TestKit runs in an isolated Gradle process,
 * gradlePluginPortal() is added to pluginManagement to resolve the KSP plugin.
 */
class KspIntegrationTest {

    companion object {
        // System properties injected from Version Catalog
        private val KOTLIN_VERSION = System.getProperty("kotlin.version") ?: "2.0.0"
        private val KSP_VERSION = System.getProperty("ksp.version") ?: "2.0.0-1.0.24"
    }

    @TempDir
    lateinit var projectDir: File

    @BeforeEach
    fun setup() {
        File(projectDir, "settings.gradle.kts").writeText("""
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                    mavenCentral()
                }
            }
            rootProject.name = "ksp-integration-test"
        """.trimIndent())
    }

    @Test
    fun `should add annotation and processor dependencies when KSP is applied`() {
        writeBuildFile("""
            tasks.register("printDependencies") {
                doLast {
                    val implDeps = configurations.getByName("implementation").dependencies
                        .map { "${'$'}{it.group}:${'$'}{it.name}" }
                    val kspDeps = configurations.getByName("ksp").dependencies
                        .map { "${'$'}{it.group}:${'$'}{it.name}" }
                    println("impl-deps=${'$'}implDeps")
                    println("ksp-deps=${'$'}kspDeps")
                }
            }
        """)

        val result = runGradle("printDependencies")

        assertTrue(result.output.contains("io.github.chloeeekim:kontract-annotation"), "annotation dependency should be added")
        assertTrue(result.output.contains("io.github.chloeeekim:kontract-processor"), "processor dependency should be added")
    }

    @Test
    fun `should add jackson dependency by default`() {
        writeBuildFile("""
            tasks.register("printDependencies") {
                doLast {
                    val implDeps = configurations.getByName("implementation").dependencies
                        .map { "${'$'}{it.group}:${'$'}{it.name}" }
                    println("impl-deps=${'$'}implDeps")
                }
            }
        """)

        val result = runGradle("printDependencies")

        assertTrue(
            result.output.contains("com.fasterxml.jackson.module:jackson-module-kotlin"),
            "jackson dependency should be added by default",
        )
    }

    @Test
    fun `should replace jackson with kotlinx when serializer is kotlinx`() {
        writeBuildFile("""
            kontract {
                serializer.set("kotlinx")
            }

            tasks.register("printDependencies") {
                doLast {
                    val implDeps = configurations.getByName("implementation").dependencies
                        .map { "${'$'}{it.group}:${'$'}{it.name}" }
                    println("impl-deps=${'$'}implDeps")
                }
            }
        """)

        val result = runGradle("printDependencies")

        assertFalse(
            result.output.contains("com.fasterxml.jackson.module:jackson-module-kotlin"),
            "jackson dependency should be removed when kotlinx is selected",
        )
        assertTrue(
            result.output.contains("org.jetbrains.kotlinx:kotlinx-serialization-json"),
            "kotlinx dependency should be added",
        )
    }

    @Test
    fun `should register KSP generated source directory`() {
        writeBuildFile("""
            tasks.register("printSourceDirs") {
                doLast {
                    val srcDirs = kotlin.sourceSets.getByName("main").kotlin.srcDirs.map { it.path }
                    println("source-dirs=${'$'}srcDirs")
                }
            }
        """)

        val result = runGradle("printSourceDirs")
        val normalizedOutput = result.output.replace("\\", "/")

        assertTrue(
            normalizedOutput.contains("build/generated/ksp/main/kotlin"),
            "KSP generated source directory should be registered",
        )
    }

    @Test
    fun `should pass serializer KSP arg without error`() {
        writeBuildFile()

        val result = runGradle("tasks")

        assertFalse(
            result.output.contains("Could not set KSP arg"),
            "KSP arg should be set without error",
        )
    }

    @Test
    fun `should not warn about KSP source directory registration`() {
        writeBuildFile()

        val result = runGradle("tasks")

        assertFalse(
            result.output.contains("Could not register KSP sources directory"),
            "KSP source directory should be registered without warning",
        )
    }

    @Test
    fun `should not warn about missing KSP when KSP is applied`() {
        writeBuildFile()

        val result = runGradle("tasks")

        assertFalse(
            result.output.contains("KSP plugin is not applied"),
            "should not warn when KSP plugin is present",
        )
    }

    @Test
    fun `should pass coroutines KSP arg without error`() {
        writeBuildFile("""
            kontract {
                coroutines.set(true)
            }
        """)

        val result = runGradle("tasks")

        assertFalse(
            result.output.contains("Could not set KSP arg"),
            "coroutines KSP arg should be set without error",
        )
    }

    @Test
    fun `should add coroutines dependency when coroutines enabled`() {
        writeBuildFile("""
            kontract {
                coroutines.set(true)
            }

            tasks.register("printDependencies") {
                doLast {
                    val implDeps = configurations.getByName("implementation").dependencies
                        .map { "${'$'}{it.group}:${'$'}{it.name}" }
                    println("impl-deps=${'$'}implDeps")
                }
            }
        """)

        val result = runGradle("printDependencies")

        assertTrue(
            result.output.contains("io.vertx:vertx-lang-kotlin-coroutines"),
            "coroutines dependency should be added when coroutines is enabled",
        )
    }

    private fun writeBuildFile(extraConfig: String = "") {
        File(projectDir, "build.gradle.kts").writeText("""
            plugins {
                kotlin("jvm") version "$KOTLIN_VERSION"
                id("com.google.devtools.ksp") version "$KSP_VERSION"
                id("io.github.chloeeekim.kontract")
            }

            repositories {
                mavenCentral()
            }

            $extraConfig
        """.trimIndent())
    }

    private fun runGradle(vararg args: String) = GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(*args)
        .forwardOutput()
        .build()
}
