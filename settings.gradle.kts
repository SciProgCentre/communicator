rootProject.name = "communicator"

pluginManagement {
    val kotlinVersion: String by settings

    repositories {
        gradlePluginPortal()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
    }

    plugins { kotlin("multiplatform") version kotlinVersion }
}

include(
    ":communicator-api",
    ":communicator-zmq",
    ":communicator-transport",
    ":demo"
)
