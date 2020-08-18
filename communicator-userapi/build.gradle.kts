import scientifik.useCoroutines

plugins { id("scientifik.jvm") }
useCoroutines()

dependencies {
    api("org.zeromq:jeromq:0.5.2")
    implementation(project(":communicator-api"))
}
