@file:Suppress("UNUSED_VARIABLE")

internal val jeromqVersion: String by project
internal val kotlinLoggingVersion: String by project
internal val statelyIsoVersion: String by project

plugins { kotlin("multiplatform") }

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
                api(project(":communicator-api"))
                api("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
                api("co.touchlab:stately-isolate:$statelyIsoVersion")
                api("co.touchlab:stately-iso-collections:$statelyIsoVersion")
            }
        }

        val jvmMain by getting {
            dependencies { api("org.zeromq:jeromq:$jeromqVersion") }
        }
    }
}
