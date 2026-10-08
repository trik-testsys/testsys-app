plugins {
    id("testsys.conventions")
    kotlin("plugin.spring")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.vaadin)
}

group = "tech.testsys.web"

repositories {
    // The grading protocol of testsys-infra:grpc is resolved from this repository.
    maven {
        url = uri("https://raw.githubusercontent.com/trik-testsys/maven-repo/releases")
        content { includeGroup("trik.testsys.grading") }
    }
}

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(platform(libs.vaadin.bom))
    implementation(project(":testsys-domain"))
    implementation(project(":testsys-operation"))
    implementation(project(":testsys-infra:database"))
    implementation(project(":testsys-infra:diagnostics"))
    implementation(project(":testsys-infra:grpc"))
    implementation(project(":testsys-web:components"))
    implementation(libs.spring.tx)
    implementation(libs.vaadin.spring.boot.starter)
    implementation(libs.kotlin.reflect)
    developmentOnly(platform(libs.vaadin.bom))
    developmentOnly(libs.vaadin.dev)

    testImplementation(libs.bundles.test.implementation)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.karibu.testing)
    testImplementation(libs.karibu.testing.spring)
    testRuntimeOnly(libs.bundles.test.runtime)
    testRuntimeOnly(libs.h2)
}

configureWebFrontend()
