@file:Suppress("UNUSED_VARIABLE")

internal val ktorVersion: String by project
plugins { kotlin(module = "multiplatform") }

kotlin {
    explicitApi()

    js {
        browser()
        nodejs()
    }

    jvm()

    sourceSets {
        all {
            with(languageSettings) {
                useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
                useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
            }
        }

        commonMain {
            dependencies { api("io.ktor:ktor-io:$ktorVersion") }
        }
    }
}
