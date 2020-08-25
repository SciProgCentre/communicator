import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

val ioVersion: String by project
plugins { kotlin(module = "multiplatform") }

kotlin {
    js()
    jvm()
    val hostOs = System.getProperty("os.name")
    val nativeTargets = mutableListOf<KotlinNativeTarget>()

    nativeTargets += when {
        hostOs == "Linux" -> linuxX64()
        hostOs.startsWith("Windows") -> mingwX64()
        else -> throw GradleException("Host OS '$hostOs' is not supported in Kotlin/Native $project.")
    }

    configure(nativeTargets) { binaries.sharedLib() }

    sourceSets {
        all { languageSettings.useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts") }
        val commonMain by getting { dependencies { api("org.jetbrains.kotlinx:kotlinx-io:$ioVersion") } }
        val nativeMain by creating { dependsOn(commonMain) }
        val nativeTest by creating { dependsOn(commonTest.get()) }

        configure(nativeTargets) {
            val main by compilations.getting { kotlinSourceSets.forEach { it.dependsOn(nativeMain) } }
            val test by compilations.getting { kotlinSourceSets.forEach { it.dependsOn(nativeTest) } }
        }
    }
}
