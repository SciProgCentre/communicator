@file:Suppress("UNUSED_VARIABLE")

internal val slf4jVersion: String by project
plugins { kotlin("multiplatform") }

kotlin {
    explicitApi()
    jvm()

    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
        }

        commonMain {
            dependencies {
                api(project(":communicator-zmq"))
            }
        }

        commonTest {
            dependencies {
                implementation("org.slf4j:slf4j-simple:$slf4jVersion")
                implementation(kotlin("test"))
            }
        }
    }
}
