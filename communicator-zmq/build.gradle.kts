@file:Suppress("KDocMissingDocumentation", "UNUSED_VARIABLE")

internal val coroutinesVersion: String by project
internal val jeromqVersion: String by project
internal val statelyIsoVersion: String by project
plugins { kotlin(module = "multiplatform") }

kotlin {
    explicitApi()
    jvm()
    val hostOs = System.getProperty("os.name")

    val nativeTarget = when {
        hostOs == "Linux" -> linuxX64()
        hostOs.startsWith("Windows") -> mingwX64()
        else -> throw GradleException("Host OS '$hostOs' is not supported in Kotlin/Native $project.")
    }

    nativeTarget.apply {
        val main by compilations.getting

        main.cinterops {
            val libczmq by creating {
                includeDirs { allHeaders("./src/nativeMain/resources/") }
            }
        }
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
                implementation("co.touchlab:stately-isolate:$statelyIsoVersion")
                implementation("co.touchlab:stately-iso-collections:$statelyIsoVersion")
            }
        }

        val jvmMain by getting { dependencies { api("org.zeromq:jeromq:$jeromqVersion") } }
        val nativeMain by creating { dependsOn(commonMain.get()) }
        val nativeTest by creating { dependsOn(commonTest.get()) }

        nativeTarget.apply {
            val main by compilations.getting { defaultSourceSet.dependsOn(nativeMain) }
            val test by compilations.getting { defaultSourceSet.dependsOn(nativeTest) }
        }
    }
}
