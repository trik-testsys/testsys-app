plugins {
    id("testsys.conventions")
}

group = "tech.testsys.web"

dependencies {
    api(platform(libs.vaadin.bom))
    api(libs.vaadin.core)

    testImplementation(libs.bundles.test.implementation)
    testImplementation(libs.karibu.testing)
    testRuntimeOnly(libs.bundles.test.runtime)
}

// The design system folder is the single copy of the CSS; the jar serves it from META-INF/resources.
tasks.processResources {
    from(layout.projectDirectory.dir("../design-system")) {
        include("styles.css", "tokens/**")
        into("META-INF/resources/design-system")
    }
}

// Browsers revalidate the stylesheets by their Last-Modified date, which Vaadin takes from the jar entry. A reproducible
// jar stamps every entry with one fixed date, so a changed stylesheet looked unchanged and stale styles stayed cached.
tasks.jar {
    isPreserveFileTimestamps = true
}
