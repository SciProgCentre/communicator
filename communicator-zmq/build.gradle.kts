@file:Suppress("UNUSED_VARIABLE")

plugins { id(miptNpm.plugins.kotlin.multiplatform.get().pluginId) }

kotlin {
    explicitApi()
    jvm()

    sourceSets {
        all {
            with(languageSettings) {
                useExperimentalAnnotation("kotlin.ExperimentalStdlibApi")
                useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
            }
        }

        commonMain {
            dependencies {
                api(projects.communicatorApi)
                api(libs.kotlin.logging)
                api(libs.stately.isolate)
                api(libs.stately.iso.collections)
            }
        }

        val jvmMain by getting {
            dependencies { api(libs.jeromq) }
        }
    }
}
