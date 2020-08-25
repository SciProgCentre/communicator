import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins { kotlin(module = "multiplatform") }

kotlin {
    js()
    jvm()
    configure(listOf<KotlinNativeTarget>(linuxX64(), mingwX64())) { binaries.sharedLib() }

    sourceSets {
        all { languageSettings.useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts") }
        val commonMain by getting {/* dependencies { api("org.jetbrains.kotlinx:kotlinx-io:0.2.0-npm-dev-10") }*/ }
        val jsMain by getting { dependencies { api("org.jetbrains.kotlinx:kotlinx-io-js:0.2.0-npm-dev-8") } }
        val jvmMain by getting { dependencies { api("org.jetbrains.kotlinx:kotlinx-io-jvm:0.2.0-npm-dev-8") } }
        val nativeMain by creating { dependsOn(commonMain) }

        val linuxX64Main by getting {
            dependsOn(nativeMain)
            dependencies { api("org.jetbrains.kotlinx:kotlinx-io-linuxx64:0.2.0-npm-dev-8") }
        }

        val mingwX64Main by getting { dependsOn(nativeMain)
            dependencies { api("org.jetbrains.kotlinx:kotlinx-io-mingwx64:0.2.0-npm-dev-8") }
        }
    }

}
