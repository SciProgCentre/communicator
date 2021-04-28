@file:Suppress("UNUSED_VARIABLE")

internal val jeromqVersion: String by project
internal val kotlinLoggingVersion: String by project
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

    nativeTarget?.compilations?.get("main")?.cinterops { val libczmq by creating }

    sourceSets {
        all {
            with(languageSettings) {
                useExperimentalAnnotation("kotlin.ExperimentalStdlibApi")
                useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
                useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
            }
        }

        commonMain {
            dependencies {
                api(project(":communicator-api"))
                api("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
                implementation("co.touchlab:stately-isolate:$statelyIsoVersion")
                implementation("co.touchlab:stately-iso-collections:$statelyIsoVersion")
            }
        }

        val jvmMain by getting { dependencies { api("org.zeromq:jeromq:$jeromqVersion") } }
        val nativeMain by creating { dependsOn(commonMain.get()) }
        val nativeTest by creating { dependsOn(commonTest.get()) }

        nativeTarget?.apply {
            val main by compilations.getting { defaultSourceSet.dependsOn(nativeMain) }
            val test by compilations.getting { defaultSourceSet.dependsOn(nativeTest) }
        }
    }
}
