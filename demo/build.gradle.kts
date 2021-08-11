@file:Suppress("UNUSED_VARIABLE")

plugins {
    id(miptNpm.plugins.kotlin.multiplatform.get().pluginId)
    id(miptNpm.plugins.kotlin.plugin.serialization.get().pluginId)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.get().dependencies { implementation(projects.communicatorZmq) }

        val jvmMain by getting {
            dependencies { implementation(libs.slf4j.simple) }
        }
    }
}
