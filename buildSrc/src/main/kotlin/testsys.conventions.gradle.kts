import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    id("io.gitlab.arturbosch.detekt")
}

group = "tech.testsys"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)

        allWarningsAsErrors = true
        jvmTarget = JvmTarget.JVM_21
    }
}

// TODO update to custom config if needed (https://detekt.dev)
tasks.withType<Detekt>().configureEach {
    reports {
        sarif.required.set(true)
    }

//    config.setFrom("$rootDir/detekt.yml")
//    buildUponDefaultConfig = true
}