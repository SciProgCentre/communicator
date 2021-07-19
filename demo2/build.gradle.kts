internal val slf4jVersion: String by project

buildscript {
    dependencies.classpath("space.kscience.communicator.prettyapi:pretty-api-gradle-plugin:0.0.1")
}

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

apply(plugin = "space.kscience.kmath.communicator.prettyapi.plugin")

dependencies {
    implementation("org.slf4j:slf4j-simple:$slf4jVersion")
    implementation(project(":communicator-zmq"))
    implementation(project(":communicator-pretty-api"))
}
