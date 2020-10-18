@file:Suppress("UNUSED_VARIABLE")

plugins { kotlin(module = "multiplatform") }

kotlin {
    jvm()

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":communicator-transport"))
                implementation(project(":communicator-zmq"))
                implementation(project(":communicator-api"))
            }
        }
    }
}
