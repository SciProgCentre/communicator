@file:Suppress("UNUSED_VARIABLE")

internal val slf4jVersion: String by project
plugins { kotlin(module = "multiplatform") }

kotlin {
    jvm()

    val nativeTarget = when (val hostOs = System.getProperty("os.name")) {
        "Linux" -> linuxX64()
        else -> null
    }

    nativeTarget?.binaries?.executable()

    sourceSets {
        commonMain.get().dependencies { implementation(project(":communicator-zmq")) }
        val jvmMain by getting { dependencies { implementation("org.slf4j:slf4j-simple:$slf4jVersion") } }
        val nativeMain by creating { dependsOn(commonMain.get()) }
        val nativeTest by creating { dependsOn(commonTest.get()) }

        nativeTarget?.apply {
            val main by compilations.getting { defaultSourceSet.dependsOn(nativeMain) }
            val test by compilations.getting { defaultSourceSet.dependsOn(nativeTest) }
        }
    }
}
