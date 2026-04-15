plugins {
    kotlin("jvm") version "2.0.0" apply false
    id("com.google.devtools.ksp") version "2.0.0-1.0.24" apply false
}

allprojects {
    group = "io.github.chloeeekim"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    dependencies {
        "testImplementation"(kotlin("test"))
        "testImplementation"("org.junit.jupiter:junit-jupiter:5.10.3")
    }
}
