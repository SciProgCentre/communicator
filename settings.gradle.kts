rootProject.name = "communicator"

pluginManagement {
    val dokkaVersion: String by settings
    val kotlinVersion: String by settings

    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.kotlin.link")
    }

    plugins {
        id("ru.mipt.npm.gradle.project") version "0.10.0"
        id("org.jetbrains.dokka") version dokkaVersion
        kotlin("multiplatform") version kotlinVersion
    }
}

include(
    ":communicator-api",
    ":communicator-zmq",
    ":demo",
)
