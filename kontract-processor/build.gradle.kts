plugins {
    `maven-publish`
    signing
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
    }
}

dependencies {
    implementation(project(":kontract-annotation"))
    implementation(libs.ksp.symbol.processing.api)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kctfork.core)
    testImplementation(libs.kctfork.ksp)
}

publishing {
    repositories {
        maven {
            name = "CentralPortal"
            url = uri("https://central.sonatype.com/api/v1/publisher")
            credentials {
                username = findProperty("mavenCentralUsername") as String?
                password = findProperty("mavenCentralPassword") as String?
            }
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            pom {
                name.set("Kontract Processor")
                description.set("Compile-time generated, type-safe API contracts for Vert.x with zero reflection overhead")
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
    val signingKeyId: String? = findProperty("signingKeyId") as String?
    val signingKey: String? = findProperty("signingKey") as String?
    val signingPassword: String? = findProperty("signingPassword") as String?

    val isRelease = !project.version.toString().endsWith("-SNAPSHOT")

    if (isRelease && signingKey != null) {
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
        sign(publishing.publications)
    }
}

tasks.withType<Sign> {
    onlyIf { !project.version.toString().endsWith("-SNAPSHOT") }
}
