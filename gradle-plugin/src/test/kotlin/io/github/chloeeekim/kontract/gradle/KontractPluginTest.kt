package io.github.chloeeekim.kontract.gradle

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KontractPluginTest {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `should apply plugin without errors`() {
        writeBuildFile()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks")
            .forwardOutput()
            .build()

        assertTrue(result.output.contains("BUILD SUCCESSFUL"))
    }

    @Test
    fun `should warn when KSP plugin is not applied`() {
        writeBuildFile()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks")
            .forwardOutput()
            .build()

        assertTrue(result.output.contains("KSP plugin is not applied"))
    }

    @Test
    fun `should register Kontract extension`() {
        writeBuildFile("""
            tasks.register("printSerializer") {
                doLast {
                    val ext = project.extensions.getByType(io.github.chloeeekim.kontract.gradle.KontractExtension::class.java)
                    println("serializer=" + ext.serializer.get())
                }
            }
        """)

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("printSerializer")
            .forwardOutput()
            .build()

        assertTrue(result.output.contains("serializer=jackson"))
    }

    @Test
    fun `should allow changing serializer via DSL`() {
        writeBuildFile("""
            kontract {
                serializer.set("kotlinx")
            }

            tasks.register("printSerializer") {
                doLast {
                    val ext = project.extensions.getByType(io.github.chloeeekim.kontract.gradle.KontractExtension::class.java)
                    println("serializer=" + ext.serializer.get())
                }
            }
        """)

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("printSerializer")
            .forwardOutput()
            .build()

        assertTrue(result.output.contains("serializer=kotlinx"))
    }

    // --- properties loading test ---

    @Test
    fun `should load version from properties file`() {
        val props = KontractPlugin.loadProperties()
        val version = props.getProperty("version")
        assertNotNull(version, "version property should be present")
        assertTrue(version.isNotBlank(), "version should not be blank")
    }

    @Test
    fun `should load jacksonVersion from properties file`() {
        val props = KontractPlugin.loadProperties()
        val jacksonVersion = props.getProperty("jacksonVersion")
        assertNotNull(jacksonVersion, "jacksonVersion property should be present")
        assertTrue(jacksonVersion.isNotBlank(), "jacksonVersion should not be blank")
    }

    @Test
    fun `should load kotlinxVersion from properties file`() {
        val props = KontractPlugin.loadProperties()
        val kotlinxVersion = props.getProperty("kotlinxVersion")
        assertNotNull(kotlinxVersion, "kotlinxVersion property should be present")
        assertTrue(kotlinxVersion.isNotBlank(), "kotlinxVersion should not be blank")
    }

    private fun writeBuildFile(extraConfig: String = "") {
        File(projectDir, "settings.gradle.kts").writeText("""
            rootProject.name = "test-project"
        """.trimIndent())

        File(projectDir, "build.gradle.kts").writeText("""
            plugins {
                kotlin("jvm") version "2.0.0"
                id("io.github.chloeeekim.kontract")
            }

            repositories {
                mavenCentral()
            }

            $extraConfig
        """.trimIndent())
    }
}
