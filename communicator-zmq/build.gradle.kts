@file:Suppress("KDocMissingDocumentation", "UNUSED_VARIABLE")

internal val coroutinesVersion: String by project
internal val jeromqVersion: String by project
internal val statelyIsoVersion: String by project
plugins { kotlin(module = "multiplatform") }

kotlin {
    explicitApi()
    jvm()

    val nativeTarget = when (val hostOs = System.getProperty("os.name")) {
        "Linux" -> linuxX64()
        "Mac OS X" -> macosX64()
        else -> throw GradleException("Host OS '$hostOs' is not supported in Kotlin/Native $project.")
    }

    val main by nativeTarget.compilations.getting { cinterops { val libczmq by creating { includeDirs("./src/nativeMain/resources/") } } }
    val test by nativeTarget.compilations.getting

    sourceSets {
        all {
            with(languageSettings) {
                useExperimentalAnnotation("kotlin.ExperimentalStdlibApi")
                useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
                useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
            }
        }

        commonMain.get().dependencies {
            api(project(":communicator-api"))
            implementation("co.touchlab:stately-isolate:$statelyIsoVersion")
            implementation("co.touchlab:stately-iso-collections:$statelyIsoVersion")
        }

        val jvmMain by getting { dependencies { api("org.zeromq:jeromq:$jeromqVersion") } }
        val nativeMain by creating { dependsOn(commonMain.get()) }
        val nativeTest by creating { dependsOn(commonTest.get()) }
        main.defaultSourceSet.dependsOn(nativeMain)
        test.defaultSourceSet.dependsOn(nativeTest)
    }
}
