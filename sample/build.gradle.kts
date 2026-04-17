plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    implementation(project(":kontract-annotation"))
    ksp(project(":kontract-processor"))

    implementation(libs.vertx.core)
    implementation(libs.vertx.web)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.vertx.lang.kotlin.coroutines)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.vertx.junit5)
    testImplementation(libs.vertx.web.client)
}

ksp {
    arg("kontract.coroutines", "true")
}