@file:Suppress("UNUSED_VARIABLE")

plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(kotlin("gradle-plugin", "1.4.31"))
}

gradlePlugin {
    val prettyApiPlugin by plugins.registering {
        id = "space.kscience.kmath.communicator.prettyapi.plugin"
        implementationClass = "space.kscience.communicator.prettyapi.gradle.CommunicatorGradleSubplugin"
    }
}
