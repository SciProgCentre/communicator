@file:Suppress("UNUSED_VARIABLE")

internal val slf4jVersion: String by project
plugins { kotlin(module = "multiplatform") }

kotlin {
    jvm()

    sourceSets {
        commonMain.get().dependencies { implementation(project(":communicator-zmq")) }

        val jvmMain by getting {
            dependencies { implementation("org.slf4j:slf4j-simple:$slf4jVersion") }
        }
    }
}
