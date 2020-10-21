@file:Suppress("UNUSED_VARIABLE")

internal val coroutinesVersion: String by project
internal val ioVersion: String by project
plugins { kotlin(module = "multiplatform") }

kotlin {
    explicitApi()

    js {
        browser()
        nodejs()
    }

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
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
            api("org.jetbrains.kotlinx:kotlinx-io:$ioVersion")
        }

        val nativeMain by creating { dependsOn(commonMain.get()) }
        val nativeTest by creating { dependsOn(commonTest.get()) }

        nativeTarget?.apply {
            val main by compilations.getting { defaultSourceSet.dependsOn(nativeMain) }
            val test by compilations.getting { defaultSourceSet.dependsOn(nativeTest) }
        }
    }
}
