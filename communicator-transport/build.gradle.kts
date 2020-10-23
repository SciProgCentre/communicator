@file:Suppress("UNUSED_VARIABLE")

internal val slf4jVersion: String by project
internal val statelyIsoVersion: String by project
plugins { kotlin(module = "multiplatform") }

kotlin {
    explicitApi()
    jvm()

    val nativeTarget = when (val hostOs = System.getProperty("os.name")) {
        "Linux" -> linuxX64()
        else -> null
    }

    sourceSets {
        all {
            with(languageSettings) {
                useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
                useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
            }
        }

        commonMain.get().dependencies {
            api(project(":communicator-api"))
            api(project(":communicator-zmq"))
            implementation("co.touchlab:stately-isolate:$statelyIsoVersion")
            implementation("co.touchlab:stately-iso-collections:$statelyIsoVersion")
        }

        commonTest.get().dependencies {
            implementation("org.slf4j:slf4j-simple:$slf4jVersion")
            implementation(kotlin("test"))
        }

        val nativeMain by creating { dependsOn(commonMain.get()) }
        val nativeTest by creating { dependsOn(commonTest.get()) }

        nativeTarget?.apply {
            val main by compilations.getting { defaultSourceSet.dependsOn(nativeMain) }
            val test by compilations.getting { defaultSourceSet.dependsOn(nativeTest) }
        }
    }
}
