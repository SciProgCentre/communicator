internal val slf4jVersion: String by project

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation("org.slf4j:slf4j-simple:$slf4jVersion")
    implementation(project(":communicator-zmq"))
    implementation(project(":communicator-pretty-api"))
}
