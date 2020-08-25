
plugins {
    id("org.jetbrains.kotlin.multiplatform") version "1.4.0"
}

kotlin {
    sourceSets {
        jvm()
        linuxX64("linux")

        val commonMain by getting {
            dependencies {
                implementation(project(":communicator-api"))
                implementation(project(":communicator-zmq"))
                implementation(project(":communicator-transport"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(project(":communicator-api"))
                implementation(project(":communicator-zmq"))
                implementation(project(":communicator-transport"))
                implementation("org.slf4j:slf4j-simple:1.7.30")
            }
        }

        val linuxMain by getting {

        }

        val dsadMain by getting {

        }

    }
}
