@file:Suppress("UNUSED_VARIABLE")

internal val slf4jVersion: String by project
plugins { kotlin(module = "multiplatform") }

kotlin {
    jvm()

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":communicator-transport"))
                implementation(project(":communicator-zmq"))
                implementation(project(":communicator-api"))
                implementation("org.slf4j:slf4j-simple:$slf4jVersion")
            }
        }
    }
}
