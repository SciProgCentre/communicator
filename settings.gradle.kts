enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("VERSION_CATALOGS")
rootProject.name = "communicator"

pluginManagement.repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://repo.kotlin.link")
}

dependencyResolutionManagement {
    repositories {
        maven("https://repo.kotlin.link")
        mavenCentral()
        mavenLocal()
        gradlePluginPortal()
    }

    versionCatalogs.create("miptNpm") {
        from("ru.mipt.npm:version-catalog:0.10.2")
    }
}

include(
    ":communicator-api",
    ":communicator-zmq",
    ":demo",
)
