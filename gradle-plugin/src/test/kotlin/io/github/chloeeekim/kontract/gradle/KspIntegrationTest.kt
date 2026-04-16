package io.github.chloeeekim.kontract.gradle

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * KSP 플러그인과 함께 적용 시 configureWithKsp 로직을 검증하는 통합 테스트.
 *
 * TestKit은 독립된 Gradle 프로세스에서 실행되므로,
 * pluginManagement에 gradlePluginPortal()을 등록하여 KSP 플러그인을 해석한다.
 */
class KspIntegrationTest {

    companion object {
        // KSP 버전은 루트 build.gradle.kts의 Kotlin/KSP 버전과 일치해야 한다.
        private const val KOTLIN_VERSION = "2.0.0"
        private const val KSP_VERSION = "2.0.0-1.0.24"
    }

    @TempDir
    lateinit var projectDir: File

    @BeforeEach
    fun setup() {
        // settings.gradle.kts — KSP 플러그인 해석을 위한 pluginManagement 설정
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
