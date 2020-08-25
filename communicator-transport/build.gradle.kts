plugins { kotlin("multiplatform") }

kotlin {
    jvm()
    js()
    configure(listOf(linuxX64(), mingwX64())) { binaries.sharedLib() }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":communicator-api"))
                api(project(":communicator-zmq"))
            }
        }

        val jsMain by getting {}
        val jvmMain by getting {}

        val nativeMain by creating {
            dependsOn(commonMain)
            dependencies { api("org.jetbrains.kotlinx:kotlinx-coroutines-core-native:1.3.8") }
        }

        val linuxX64Main by getting { dependsOn(nativeMain) }
        val mingwX64Main by getting { dependsOn(nativeMain) }
    }
}
