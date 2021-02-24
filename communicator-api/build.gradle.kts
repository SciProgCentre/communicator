@file:Suppress("UNUSED_VARIABLE")

internal val coroutinesVersion: String by project
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

    val nativeTargets = (when {
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
    }) + listOf(linuxX64())

    sourceSets {
        all {
            with(languageSettings) {
                useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
                useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
                useExperimentalAnnotation("kotlinx.coroutines.InternalCoroutinesApi")
            }
        }

        commonMain { dependencies { api("io.ktor:ktor-io:$ktorVersion") } }

        val nativeMain by creating { dependsOn(commonMain.get()) }
        val nativeTest by creating { dependsOn(commonTest.get()) }

        configure(nativeTargets) {
            val main by compilations.getting { defaultSourceSet.dependsOn(nativeMain) }
            val test by compilations.getting { defaultSourceSet.dependsOn(nativeTest) }
        }
    }
}
