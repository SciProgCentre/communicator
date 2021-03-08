@file:Suppress("UNUSED_VARIABLE")

internal val slf4jVersion: String by project
internal val statelyIsoVersion: String by project
plugins { kotlin("multiplatform") }

kotlin {
    explicitApi()
    jvm()

    val nativeTarget = when (System.getProperty("os.name")) {
        "Mac OS X" -> macosX64()
        "Linux" -> linuxX64()
        else -> null
    }

    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
        }

        commonMain {
            dependencies {
                api(project(":communicator-zmq"))
                implementation("co.touchlab:stately-isolate:$statelyIsoVersion")
                implementation("co.touchlab:stately-iso-collections:$statelyIsoVersion")
            }
        }

        commonTest {
            dependencies {
                implementation("org.slf4j:slf4j-simple:$slf4jVersion")
                implementation(kotlin("test"))
            }
        }

        val nativeMain by creating { dependsOn(commonMain.get()) }
        val nativeTest by creating { dependsOn(commonTest.get()) }

        nativeTarget?.apply {
            val main by compilations.getting { defaultSourceSet.dependsOn(nativeMain) }
            val test by compilations.getting { defaultSourceSet.dependsOn(nativeTest) }
        }
    }
}
