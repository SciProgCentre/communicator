plugins { kotlin(module = "jvm") }

dependencies {
    implementation(project(":communicator-transport"))
    implementation(project(":communicator-zmq"))
    implementation(project(":communicator-api"))
}
