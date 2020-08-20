import scientifik.useCoroutines

plugins { id("scientifik.mpp") }
useCoroutines()

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":communicator-api"))
                implementation(project(":communicator-zmq"))
                implementation(project(":communicator-factories"))
            }
        }

        jvmMain {
            dependencies {
                implementation(project(":communicator-api"))
                implementation(project(":communicator-zmq"))
                implementation(project(":communicator-factories"))
                implementation("org.slf4j:slf4j-simple:1.7.30")
            }
        }
    }
}
