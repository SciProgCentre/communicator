@file:Suppress("KDocMissingDocumentation")

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

val coroutinesVersion: String by project
val statelyIsoVersion: String by project
plugins { kotlin(module = "multiplatform") }

kotlin {
    jvm()
    val hostOs = System.getProperty("os.name")
    val nativeTargets = mutableListOf<KotlinNativeTarget>()
    var hasLinux = false
    var hasWindows = false

    nativeTargets += when {
        hostOs == "Linux" -> {
            hasLinux = true
            linuxX64()
        }

        hostOs.startsWith("Windows") -> {
            hasWindows = true
            mingwX64()
        }

        else -> throw GradleException("Host OS '$hostOs' is not supported in Kotlin/Native $project.")
    }

    configure(nativeTargets) {
        val main by compilations.getting
        binaries.sharedLib()

        main.cinterops {
            val libczmq by creating {
                defFile("src/nativeInterop/cinterop/libczmq.def")
                packageName("czmq")
                includeDirs { allHeaders("./src/nativeMain/resources/") }
            }
        }
    }

    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kotlin.ExperimentalStdlibApi")
            languageSettings.useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
        }

        val commonMain by getting {
            dependencies {
                api(project(":communicator-api"))
                implementation("co.touchlab:stately-isolate:$statelyIsoVersion")
                implementation("co.touchlab:stately-iso-collections:$statelyIsoVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
            }
        }

        val jvmMain by getting {
            dependencies {
                api(project(":communicator-api"))
                api("org.zeromq:jeromq:0.5.2")
                implementation("co.touchlab:stately-isolate-jvm:$statelyIsoVersion")
                implementation("co.touchlab:stately-iso-collections-jvm:$statelyIsoVersion")
            }
        }

        val nativeMain by creating { dependsOn(commonMain) }
        val nativeTest by creating { dependsOn(commonTest.get()) }

        if (hasLinux) {
            val linuxX64Main by getting {
                dependencies {
                    implementation("co.touchlab:stately-isolate-linuxx64:$statelyIsoVersion")
                    implementation("co.touchlab:stately-iso-collections-linuxx64:$statelyIsoVersion")
                }
            }
        }

        if (hasWindows) {
            val mingwX64Main by getting {
                dependencies {
                    implementation("co.touchlab:stately-isolate-mingwx64:$statelyIsoVersion")
                    implementation("co.touchlab:stately-iso-collections-mingwx64:$statelyIsoVersion")
                }
            }
        }

        configure(nativeTargets) {
            val main by compilations.getting { kotlinSourceSets.forEach { it.dependsOn(nativeMain) } }
            val test by compilations.getting { kotlinSourceSets.forEach { it.dependsOn(nativeTest) } }
        }
    }
}
