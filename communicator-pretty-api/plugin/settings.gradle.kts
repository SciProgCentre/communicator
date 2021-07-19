rootProject.name = "plugin"

pluginManagement {
    repositories {
        maven("https://repo.kotlin.link")
        mavenCentral()
        gradlePluginPortal()
    }

    val toolsVersion = "0.10.2"

    plugins { id("ru.mipt.npm.gradle.project") version toolsVersion }
}

include(":pretty-api-compiler-plugin", ":pretty-api-gradle-plugin")
