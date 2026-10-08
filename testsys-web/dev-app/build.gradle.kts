plugins {
    id("testsys.conventions")
    kotlin("plugin.spring")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.vaadin)
}

group = "tech.testsys.web"

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(platform(libs.vaadin.bom))
    implementation(project(":testsys-web:components"))
    implementation(libs.vaadin.spring.boot.starter)
    implementation(libs.kotlin.reflect)
    developmentOnly(platform(libs.vaadin.bom))
    developmentOnly(libs.vaadin.dev)

    testImplementation(libs.bundles.test.implementation)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.karibu.testing)
    testImplementation(libs.karibu.testing.spring)
    testRuntimeOnly(libs.bundles.test.runtime)
}

configureWebFrontend()
