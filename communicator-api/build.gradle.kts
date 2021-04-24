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
    val hostOs = System.getProperty("os.name")

    val nativeTargets = when {
        hostOs == "Mac OS X" -> listOf(iosX64(),
            iosArm32(),
            iosArm64(),
            macosX64(),
            watchosX86(),
            watchosArm64(),
            watchosArm32(),
            tvosX64(),
            tvosArm64())

        hostOs.startsWith("Windows") -> listOf(mingwX64())
        else -> emptyList()
    } + listOf(linuxX64())

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

        val nativeMain by creating { dependsOn(commonMain.get()) }
        val nativeTest by creating { dependsOn(commonTest.get()) }

        configure(nativeTargets) {
            val main by compilations.getting { defaultSourceSet.dependsOn(nativeMain) }
            val test by compilations.getting { defaultSourceSet.dependsOn(nativeTest) }
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
