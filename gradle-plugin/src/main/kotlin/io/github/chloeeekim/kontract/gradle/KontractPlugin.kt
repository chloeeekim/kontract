package io.github.chloeeekim.kontract.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.Properties

class KontractPlugin : Plugin<Project> {

    companion object {
        private const val PROPERTIES_FILE = "kontract.properties"
        private val VALID_SERIALIZERS = setOf("jackson", "kotlinx")

        internal fun loadProperties(): Properties {
            val props = Properties()
            val stream = KontractPlugin::class.java.classLoader.getResourceAsStream(PROPERTIES_FILE)
            if (stream != null) {
                stream.use { props.load(it) }
            } else {
                props.setProperty("_loadFailed", "true")
            }
            return props
        }
    }

    private val properties = loadProperties()
    private val kontractVersion: String get() = properties.getProperty("version", "0.1.0-SNAPSHOT")
    private val jacksonVersion: String get() = properties.getProperty("jacksonVersion", "2.17.2")
    private val kotlinxVersion: String get() = properties.getProperty("kotlinxVersion", "1.6.3")

    private val annotationArtifact: String get() = "io.github.chloeeekim:kontract-annotation:$kontractVersion"
    private val processorArtifact: String get() = "io.github.chloeeekim:kontract-processor:$kontractVersion"
    private val jacksonArtifact: String get() = "com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion"
    private val kotlinxArtifact: String get() = "org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxVersion"


    override fun apply(project: Project) {
        // 0. properties 로딩 실패 경고
        if (properties.getProperty("_loadFailed") == "true") {
            project.logger.warn(
                "kontract: Could not load ${PROPERTIES_FILE}. Using fallback default versions. " +
                        "This may indicate a packaging issue."
            )
        }

        // 1. DSL extension 등록
        val extension = project.extensions.create("kontract", KontractExtension::class.java)
        extension.serializer.convention("jackson")

        // 2. KSP 플러그인이 적용되면 설정 자동 구성
        project.pluginManager.withPlugin("com.google.devtools.ksp") {
            configureWithKsp(project, extension)
        }

        // 3. KSP 없이 사용하는 경우 — 경고
        project.afterEvaluate {
            if (!project.plugins.hasPlugin("com.google.devtools.ksp")) {
                project.logger.warn(
                    "kontract: KSP plugin is not applied. Apply it manually: plugins { id(\"com.google.devtools.ksp\") version \"...\" }"
                )
            }
        }
    }

    private fun configureWithKsp(project: Project, extension: KontractExtension) {
        // 의존성 즉시 추가 (afterEvaluate 밖에서)
        project.dependencies.add("implementation", annotationArtifact)
        project.dependencies.add("ksp", processorArtifact)
        // 기본 serializer(jackson) 의존성도 즉시 추가
        project.dependencies.add("implementation", jacksonArtifact)

        // KSP 생성 소스 디렉토리를 소스셋에 등록 (IDE 인식)
        registerKspSourceDir(project)

        // afterEvaluate에서 serializer 변경 사항 처리
        project.afterEvaluate {
            val serializer = extension.serializer.get()

            // 잘못된 serializer 값 검증
            if (serializer !in VALID_SERIALIZERS) {
                project.logger.warn(
                    "kontract: Unknown serializer '$serializer'. Use 'jackson' or 'kotlinx'. Falling back to 'jackson'."
                )
            }

            // KSP 옵션 전달
            configureKspArg(project, serializer)

            // kotlinx 선택 시 jackson 제거, kotlinx 추가
            if (serializer == "kotlinx") {
                project.configurations.getByName("implementation").dependencies.removeIf {
                    it.group == "com.fasterxml.jackson.module" && it.name == "jackson-module-kotlin"
                }
                project.dependencies.add("implementation", kotlinxArtifact)
            }
        }
    }

    // KotlinProjectExtension을 직접 참조하면 TestKit의 injected classloader에서
    // NoClassDefFoundError가 발생하므로 리플렉션으로 접근한다.
    // KGP(Kotlin Gradle Plugin) 2.0.0 기준 API. KGP API 변경 시 silent failure → catch + warning.
    private fun registerKspSourceDir(project: Project) {
        try {
            val kotlinExtension = project.extensions.findByName("kotlin") ?: return
            val sourceSets = kotlinExtension.javaClass.getMethod("getSourceSets").invoke(kotlinExtension)
            val mainSourceSet = sourceSets.javaClass.getMethod("getByName", String::class.java)
                .invoke(sourceSets, "main")
            val kotlinDirSet = mainSourceSet.javaClass.getMethod("getKotlin").invoke(mainSourceSet)
            kotlinDirSet.javaClass.getMethod("srcDir", Any::class.java)
                .invoke(kotlinDirSet, "build/generated/ksp/main/kotlin")
        } catch (_: Exception) {
            project.logger.warn(
                "kontract: Could not register KSP sources directory. " +
                        "Add manually: kotlin { sourceSets.main { kotlin.srcDir(\"build/generated/ksp/main/kotlin\") } }"
            )
        }
    }

    private fun configureKspArg(project: Project, serializer: String) {
        try {
            val kspExtension = project.extensions.findByName("ksp") ?: return
            val argMethod = kspExtension.javaClass.getMethod("arg", String::class.java, String::class.java)
            argMethod.invoke(kspExtension, "kontract.serializer", serializer)
        } catch (_: Exception) {
            project.logger.warn(
                "kontract: Could not set KSP arg. " +
                        "Set it manually: ksp { arg(\"kontract.serializer\", \"$serializer\") }"
            )
        }
    }
}
