plugins { kotlin(module = "jvm") }

dependencies {
    implementation(project(":communicator-api"))
    implementation(project(":communicator-zmq"))
    implementation(project(":communicator-transport"))
}
