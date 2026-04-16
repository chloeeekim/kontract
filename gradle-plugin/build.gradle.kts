plugins {
    `java-gradle-plugin`
    kotlin("jvm")
}

val jacksonVersion = "2.17.2"
val kotlinxVersion = "1.6.3"

dependencies {
    implementation(gradleApi())
    implementation(kotlin("stdlib"))
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0")
    testImplementation(gradleTestKit())
}

tasks.processResources {
    filesMatching("kontract.properties") {
        expand(
            "version" to project.version.toString(),
            "jacksonVersion" to jacksonVersion,
            "kotlinxVersion" to kotlinxVersion,
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
