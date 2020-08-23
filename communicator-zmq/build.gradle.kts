import scientifik.useCoroutines

plugins { id("scientifik.mpp") }
useCoroutines()

kotlin {
    linuxX64("linux") {
        val main by compilations.getting

        binaries.sharedLib()

        main.cinterops {
            val libczmq by creating {
                defFile("src/nativeInterop/cinterop/libczmq.def")
                packageName("czmq")
                includeDirs { allHeaders("./src/linuxMain/resources/") }
            }
        }
    }

    sourceSets {
        all { languageSettings.useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts") }

        commonMain {
            dependencies {
                api(project(":communicator-api"))
                implementation("co.touchlab:stately-isolate:1.0.3-a4")
                implementation("co.touchlab:stately-iso-collections:1.0.3-a4")
            }
        }

        jsMain {
            dependencies {
                implementation("co.touchlab:stately-isolate-js:1.0.3-a4")
                implementation("co.touchlab:stately-iso-collections-js:1.0.3-a4")
            }
        }

        jvmMain {
            dependencies {
                api("org.zeromq:jeromq:0.5.2")
                implementation("co.touchlab:stately-isolate-jvm:1.0.3-a4")
                implementation("co.touchlab:stately-iso-collections-jvm:1.0.3-a4")
            }
        }

        val linuxMain by getting {
            dependencies {
                implementation("co.touchlab:stately-isolate-linuxx64:1.0.3-a4")
                implementation("co.touchlab:stately-iso-collections-linuxx64:1.0.3-a4")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core-native:1.3.7")
            }
        }
    }
}
