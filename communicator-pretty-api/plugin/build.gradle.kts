plugins {
    id("ru.mipt.npm.gradle.project")
}

allprojects {
    group = "space.kscience.communicator.prettyapi"
    version = "0.0.1"

    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.google.com")
        maven("https://plugins.gradle.org/m2/")
    }
}
