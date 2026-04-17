plugins {
    `maven-publish`
    signing
}

dependencies {
    compileOnly(libs.vertx.web)
    testImplementation(libs.junit.jupiter)
    testImplementation(kotlin("reflect"))
}

publishing {
    repositories {
        maven {
            name = "CentralPortal"
            url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
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
                name.set("Kontract Annotation")
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
    val signingKey = findProperty("signingKey") as String?
    val signingPassword = findProperty("signingPassword") as String?

    if (!signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    onlyIf {
        !project.version.toString().endsWith("-SNAPSHOT")
    }
}
