rootProject.name = "testsys-app"

// Gradle skips files and directories named `%*%` by default, and the user changelog keeps unreleased changes in
// `%CURRENT_VERSION%` directories.
org.apache.tools.ant.DirectoryScanner.removeDefaultExclude("**/%*%")

include(":testsys-domain")
include(":testsys-operation")
include(":testsys-web:components")
include(":testsys-web:app")
include(":testsys-infra:database")
include(":testsys-infra:database:codegen-api")
include(":testsys-infra:database:codegen")
include(":testsys-infra:grpc")
include(":testsys-infra:localization")
include(":testsys-infra:diagnostics")
include(":testsys-infra:mail")
include(":testsys-infra:crypto")
include(":testsys-infra:settings")
include(":testsys-infra:monitoring")
