import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins { kotlin("multiplatform") }

kotlin {
    jvm()
    val hostOs = System.getProperty("os.name")
    val nativeTargets = mutableListOf<KotlinNativeTarget>()

    nativeTargets += when {
        hostOs == "Linux" -> linuxX64()
        hostOs.startsWith("Windows") -> mingwX64()
        else -> throw GradleException("Host OS '$hostOs' is not supported in Kotlin/Native $project.")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":communicator-api"))
                api(project(":communicator-zmq"))
            }
        }

        val nativeMain by creating { dependsOn(commonMain) }
        val nativeTest by creating { dependsOn(commonTest.get()) }

        configure(nativeTargets) {
            val main by compilations.getting { kotlinSourceSets.forEach { it.dependsOn(nativeMain) } }
            val test by compilations.getting { kotlinSourceSets.forEach { it.dependsOn(nativeTest) } }
        }
    }
}
