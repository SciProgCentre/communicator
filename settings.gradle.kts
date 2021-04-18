rootProject.name = "communicator"

pluginManagement {
    val dokkaVersion: String by settings
    val kotlinVersion: String by settings

    plugins {
        id("org.jetbrains.dokka") version dokkaVersion
        kotlin("multiplatform") version kotlinVersion
    }
}

include(
    ":communicator-api",
    ":communicator-zmq",
    ":demo"
)
