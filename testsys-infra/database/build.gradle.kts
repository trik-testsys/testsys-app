plugins {
    id("testsys.infra-conventions")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
}

val libs = versionCatalogs.named("libs")

dependencies {
    implementation(project(":testsys-domain"))
    implementation(libs.findLibrary("spring-boot-starter-data-jpa").get())
}
