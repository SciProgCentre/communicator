@file:Suppress("UNUSED_VARIABLE")

plugins { kotlin("multiplatform") }

kotlin {
    explicitApi()

    val nativeTarget = when (val hostOs = System.getProperty("os.name")) {
        "Mac OS X" -> macosX64()
        "Linux" -> linuxX64()
        else -> null
    }


    nativeTarget?.binaries?.sharedLib("commzmq")

    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
        }

        commonMain {
            dependencies { api(project(":communicator-zmq")) }
        }

        val nativeMain by creating { dependsOn(commonMain.get()) }
        val nativeTest by creating { dependsOn(commonTest.get()) }

        nativeTarget?.apply {
            val main by compilations.getting { defaultSourceSet.dependsOn(nativeMain) }
            val test by compilations.getting { defaultSourceSet.dependsOn(nativeTest) }
        }
    }
}
