plugins {
    id("testsys.conventions")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
}

group = "tech.testsys.infra"

val springBootVersion = "3.4.3"

dependencies {
    implementation(project(":testsys-domain"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:$springBootVersion")
}
