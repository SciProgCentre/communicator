@file:Suppress("UNUSED_VARIABLE")

internal val jeromqVersion: String by project
internal val kotlinLoggingVersion: String by project
internal val statelyIsoVersion: String by project
plugins { kotlin("multiplatform") }

kotlin {
    explicitApi()
    jvm()

    val nativeTargets = listOf(
        iosX64(),
        iosArm32(),
        iosArm64(),
        macosX64(),
        watchosX86(),
        watchosArm64(),
        watchosArm32(),
        tvosX64(),
        tvosArm64(),
        linuxX64(),
    )

    configure(nativeTargets) {
        compilations["main"]?.cinterops { val libczmq by creating }
    }

    sourceSets {
        all {
            with(languageSettings) {
                useExperimentalAnnotation("kotlin.ExperimentalStdlibApi")
                useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
                useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
            }
        }

        commonMain {
            dependencies {
                api(project(":communicator-api"))
                api("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
                implementation("co.touchlab:stately-isolate:$statelyIsoVersion")
                implementation("co.touchlab:stately-iso-collections:$statelyIsoVersion")
            }
        }

        val jvmMain by getting { dependencies { api("org.zeromq:jeromq:$jeromqVersion") } }
        val nativeMain by creating { dependsOn(commonMain.get()) }
        val nativeTest by creating { dependsOn(commonTest.get()) }

        configure(nativeTargets) {
            val main by compilations.getting { defaultSourceSet.dependsOn(nativeMain) }
            val test by compilations.getting { defaultSourceSet.dependsOn(nativeTest) }
        }
    }
}
