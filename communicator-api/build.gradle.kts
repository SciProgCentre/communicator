@file:Suppress("UNUSED_VARIABLE")

internal val junitVersion: String by project
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
