rootProject.name = "communicator"

pluginManagement {
    val gradleToolsVersion: String by settings
    val kotlinVersion: String by settings

    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.kotlin.link")
    }

    plugins {
        id("ru.mipt.npm.gradle.project") version gradleToolsVersion
        kotlin("multiplatform") version kotlinVersion
    }
}

include(
    ":communicator-api",
    ":communicator-pretty-api",
    ":communicator-zmq",
    ":demo",
    ":demo2",
)

includeBuild("communicator-pretty-api/plugin")
