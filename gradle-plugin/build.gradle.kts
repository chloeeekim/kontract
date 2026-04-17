plugins {
    `java-gradle-plugin`
    kotlin("jvm")
    `maven-publish`
    signing
    alias(libs.plugins.plugin.publish)
}

dependencies {
    implementation(gradleApi())
    compileOnly(libs.kotlin.gradle.plugin)
    testImplementation(libs.junit.jupiter)
    testImplementation(gradleTestKit())
}

java {
    withSourcesJar()
    withJavadocJar()
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
            "vertxVersion" to libs.versions.vertx.get(),
        )
    }
}

gradlePlugin {
    website.set("https://github.com/chloeeekim/kontract")
    vcsUrl.set("https://github.com/chloeeekim/kontract")

    plugins {
        create("kontract") {
            id = "io.github.chloeeekim.kontract"
            displayName = "Kontract Gradle Plugin"
            description = "Auto-configures KSP and dependencies for Kontract"
            tags.set(listOf("vertx", "ksp", "code-generation", "kotlin"))
            implementationClass = "io.github.chloeeekim.kontract.gradle.KontractPlugin"
        }
    }
}

publishing {
    publications {
        withType<MavenPublication>().configureEach {
            if (name == "pluginMaven") {
                artifactId = "kontract-gradle-plugin"
            }

            pom {
                name.set("Kontract Gradle Plugin")
                description.set("Gradle plugin for auto-configuring Kontract KSP dependencies")
                url.set("https://github.com/chloeeekim/kontract")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("chloeeekim")
                        name.set("Chloe Jungah Kim")
                        url.set("https://github.com/chloeeekim")
                    }
                }
                scm {
                    url.set("https://github.com/chloeeekim/kontract")
                    connection.set("scm:git:git://github.com/chloeeekim/kontract.git")
                    developerConnection.set("scm:git:ssh://github.com/chloeeekim/kontract.git")
                }
            }
        }
    }
}

signing {
    val signingKey: String? = findProperty("signingKey") as String?
    val signingPassword: String? = findProperty("signingPassword") as String?

    if (!signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications.matching { it.name == "pluginMaven" })
    }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    onlyIf {
        !project.version.toString().endsWith("-SNAPSHOT")
    }
}
