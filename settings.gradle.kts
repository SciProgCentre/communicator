rootProject.name = "communicator"

pluginManagement {
    val kotlinVersion: String by settings
    plugins { kotlin("multiplatform") version kotlinVersion }
}

include(
    ":communicator-api",
    ":communicator-zmq",
    ":communicator-transport",
    ":communicator-rsocket",
    ":demo"
)
