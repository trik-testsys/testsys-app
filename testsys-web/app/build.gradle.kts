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
    implementation(project(":testsys-infra:settings"))
    implementation(project(":testsys-operation"))
    implementation(project(":testsys-infra:database"))
    implementation(project(":testsys-infra:diagnostics"))
    implementation(project(":testsys-infra:grpc"))
    implementation(project(":testsys-infra:mail"))
    implementation(project(":testsys-infra:monitoring"))
    implementation(project(":testsys-web:components"))
    implementation(libs.bundles.icu.implementation)
    implementation(libs.spring.tx)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.liquibase)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.vaadin.spring.boot.starter)
    implementation(libs.kotlin.reflect)
    developmentOnly(platform(libs.vaadin.bom))
    developmentOnly(libs.vaadin.dev)

    testImplementation(libs.bundles.test.implementation)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.karibu.testing)
    testImplementation(libs.karibu.testing.spring)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testRuntimeOnly(libs.bundles.test.runtime)
}

configureWebFrontend()

tasks.withType<Test>().configureEach {
    systemProperty("testsys.deploy.path", rootProject.file("deploy").absolutePath)
    inputs.file(rootProject.file("deploy/compose.monitoring.yml"))
    inputs.file(rootProject.file("deploy/config/prometheus.yml"))
    inputs.file(rootProject.file("deploy/config/loki.yml"))
    inputs.file(rootProject.file("deploy/config/alloy.alloy"))
    inputs.dir(rootProject.file("deploy/config/grafana/dashboards"))
    // A JVM zone other than the default user zone reveals dates read in the JVM zone instead of the zone of the user.
    jvmArgs("-Duser.timezone=UTC")
}
