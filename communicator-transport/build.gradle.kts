import scientifik.useCoroutines

plugins { id("scientifik.mpp") }
useCoroutines()

kotlin.sourceSets.commonMain {
    dependencies {
        api(project(":communicator-api"))
        api(project(":communicator-zmq"))
    }
}
