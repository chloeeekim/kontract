plugins {
    `java-gradle-plugin`
    kotlin("jvm")
}

dependencies {
    implementation(gradleApi())
    compileOnly(libs.kotlin.gradle.plugin)
    testImplementation(libs.junit.jupiter)
    testImplementation(gradleTestKit())
}

tasks.test {
    systemProperty("kotlin.version", libs.versions.kotlin.get())
    systemProperty("ksp.version", libs.versions.ksp.get())
}

tasks.processResources {
    filesMatching("kontract.properties") {
        expand(
            "version" to project.version.toString(),
            "jacksonVersion" to libs.versions.jackson.get(),
            "kotlinxVersion" to libs.versions.kotlinx.serialization.get(),
        )
    }
}

gradlePlugin {
    plugins {
        create("kontract") {
            id = "io.github.chloeeekim.kontract"
            implementationClass = "io.github.chloeeekim.kontract.gradle.KontractPlugin"
        }
    }
}
