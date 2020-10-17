@file:Suppress("UNUSED_VARIABLE")

internal val ioVersion: String by project
plugins { kotlin(module = "multiplatform") }

kotlin {
    explicitApi()

    js {
        browser()
        nodejs()
    }

    jvm()
    val hostOs = System.getProperty("os.name")

    val nativeTarget = when {
        hostOs == "Linux" -> linuxX64()
        hostOs.startsWith("Windows") -> mingwX64()
        else -> throw GradleException("Host OS '$hostOs' is not supported in Kotlin/Native $project.")
    }

    sourceSets {
        all {
            with(languageSettings) {
                useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
                useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
            }
        }

        val commonMain by getting { dependencies { api("org.jetbrains.kotlinx:kotlinx-io:$ioVersion") } }
        val nativeMain by creating { dependsOn(commonMain) }
        val nativeTest by creating { dependsOn(commonTest.get()) }

        nativeTarget.apply {
            val main by compilations.getting { defaultSourceSet.dependsOn(nativeMain) }
            val test by compilations.getting { defaultSourceSet.dependsOn(nativeTest) }
        }
    }
}
