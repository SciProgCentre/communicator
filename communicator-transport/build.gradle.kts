@file:Suppress("UNUSED_VARIABLE")

plugins { kotlin("multiplatform") }

internal val statelyIsoVersion: String by project

kotlin {
    explicitApi()
    jvm()

    val nativeTarget = when (val hostOs = System.getProperty("os.name")) {
        "Linux" -> linuxX64()
        "Mac OS X" -> macosX64()
        else -> throw GradleException("Host OS '$hostOs' is not supported in Kotlin/Native $project.")
    }

    sourceSets {
        all { languageSettings.useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes") }

        commonMain.get().dependencies {
            api(project(":communicator-api"))
            api(project(":communicator-zmq"))
            implementation("co.touchlab:stately-isolate:$statelyIsoVersion")
            implementation("co.touchlab:stately-iso-collections:$statelyIsoVersion")
        }

        commonTest.get().dependencies { implementation(kotlin("test")) }
        val nativeMain by creating { dependsOn(commonMain.get()) }
        val nativeTest by creating { dependsOn(commonTest.get()) }

        nativeTarget.apply {
            val main by compilations.getting { defaultSourceSet.dependsOn(nativeMain) }
            val test by compilations.getting { defaultSourceSet.dependsOn(nativeTest) }
        }
    }
}
