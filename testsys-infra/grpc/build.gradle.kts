plugins {
    id("testsys.conventions")
    kotlin("plugin.spring")
}

repositories {
    maven {
        url = uri("https://raw.githubusercontent.com/trik-testsys/maven-repo/releases")
        content { includeGroup("trik.testsys.grading") }
    }
}

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(project(":testsys-domain"))
    implementation(libs.grading.protos)
    implementation(libs.grpc.okhttp)
    implementation(libs.spring.context)
    implementation(libs.spring.tx)
    implementation(libs.jackson.databind)
    testImplementation(libs.grpc.inprocess)
    testImplementation(libs.bundles.test.implementation)
    testRuntimeOnly(libs.bundles.test.runtime)
}
