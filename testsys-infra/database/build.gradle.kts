plugins {
    id("testsys.conventions")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
}

group = "tech.testsys.infra"

dependencies {
    implementation(project(":testsys-domain"))
    implementation(libs.bundles.database.implementation)
    runtimeOnly(libs.com.mysql.mysql.connector.j)
}
