import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins { kotlin("multiplatform") }

kotlin {
    jvm()
    js()
    var nativeTargets: List<KotlinNativeTarget> = mutableListOf<KotlinNativeTarget>(linuxX64(), mingwX64())

    configure(nativeTargets) {
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

        val commonMain by getting {
            dependencies {
                api(project(":communicator-api"))
                implementation("co.touchlab:stately-isolate:1.1.0-a1")
                implementation("co.touchlab:stately-iso-collections:1.1.0-a1")
            }
        }

        val jsMain by getting {
            dependencies {
                api(project(":communicator-api"))
                implementation("co.touchlab:stately-isolate-js:1.1.0-a1")
                implementation("co.touchlab:stately-iso-collections-js:1.1.0-a1")
            }
        }

        val jvmMain by getting {
            dependencies {
                api(project(":communicator-api"))
                api("org.zeromq:jeromq:0.5.2")
                implementation("co.touchlab:stately-isolate-jvm:1.1.0-a1")
                implementation("co.touchlab:stately-iso-collections-jvm:1.1.0-a1")
            }
        }

        val nativeMain by creating {
            dependsOn(commonMain)
        }

        val linuxX64Main by getting {
            dependsOn(nativeMain)

            dependencies {
                implementation("co.touchlab:stately-isolate-linuxx64:1.1.0-a1")
                implementation("co.touchlab:stately-iso-collections-linuxx64:1.1.0-a1")
            }
        }

        val mingwX64Main by getting {
            dependsOn(nativeMain)

            dependencies {
                implementation("co.touchlab:stately-isolate-mingwx64:1.1.0-a1")
                implementation("co.touchlab:stately-iso-collections-mingwx64:1.1.0-a1")
            }
        }

//        configure(nativeTargets) {
//            val main by compilations
//            val test by compilations
//            val tName = name.toLowerCase()
//
//            main.defaultSourceSet.apply {
//                kotlin.srcDirs.clear()
//                kotlin.srcDir(File(project.path, "src/nativeMain/kotlin").absolutePath)
//
//                dependencies {
//                    api("org.jetbrains.kotlinx:kotlinx-coroutines-core-native:1.3.7")
//                    implementation("co.touchlab:stately-isolate-$tName:1.0.3-a4")
//                    implementation("co.touchlab:stately-iso-collections-$tName:1.0.3-a4")
//                }
//            }
//
//            test.defaultSourceSet.apply {
//                kotlin.srcDirs.clear()
//                kotlin.srcDir(File(project.path, "src/nativeTest/kotlin").absolutePath)
//            }
//        }
    }
}
