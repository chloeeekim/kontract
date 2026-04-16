plugins {
    `java-gradle-plugin`
    kotlin("jvm")
}

dependencies {
    implementation(gradleApi())
    implementation(kotlin("stdlib"))
    compileOnly(libs.kotlin.gradle.plugin)
    testImplementation(libs.junit.jupiter)
    testImplementation(gradleTestKit())
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
