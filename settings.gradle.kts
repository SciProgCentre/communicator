rootProject.name = "communicator"

pluginManagement {
    val kotlinVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("multiplatform") version kotlinVersion
    }
}

include(
    ":communicator-api",
    ":communicator-zmq",
    ":communicator-transport",
    ":demo"
)
