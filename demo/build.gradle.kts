plugins { id("scientifik.jvm") }

dependencies {
    implementation(project(":communicator-api"))
    implementation(project(":communicator-zmq"))
    implementation(project(":communicator-transport"))
    implementation("org.slf4j:slf4j-simple:1.7.30")
}
