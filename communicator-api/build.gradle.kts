@file:Suppress("UNUSED_VARIABLE")

internal val junitVersion: String by project
internal val ktorVersion: String by project
internal val serializationVersion: String by project

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
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
                api("org.jetbrains.kotlinx:kotlinx-serialization-cbor:$serializationVersion")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
                api("io.ktor:ktor-io:$ktorVersion")
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
                implementation("org.junit.jupiter:junit-jupiter:$junitVersion")
            }
        }

        val jsTest by getting {
            dependencies { implementation(kotlin("test-js")) }
        }
    }
}

tasks.withType(Test::useJUnitPlatform)
