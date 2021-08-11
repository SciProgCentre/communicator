@file:Suppress("UNUSED_VARIABLE")

plugins {
    id(miptNpm.plugins.kotlin.multiplatform.get().pluginId)
    id(miptNpm.plugins.kotlin.plugin.serialization.get().pluginId)
}

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
                useExperimentalAnnotation("kotlinx.serialization.ExperimentalSerializationApi")
                useExperimentalAnnotation("kotlinx.serialization.InternalSerializationApi")
            }
        }

        commonMain {
            dependencies {
                api(miptNpm.kotlinx.serialization.cbor)
                api(miptNpm.kotlinx.serialization.json)
                api(miptNpm.ktor.io)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation(libs.junit.jupiter)
            }
        }

        val jsTest by getting {
            dependencies { implementation(kotlin("test-js")) }
        }
    }
}

tasks.withType(Test::useJUnitPlatform)
