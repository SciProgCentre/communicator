import scientifik.useCoroutines

plugins { id("scientifik.mpp") }
useCoroutines()

kotlin.sourceSets {
    commonMain {
        dependencies {
            implementation(project(":communicator-api"))
        }
    }

    jvmMain {
        dependencies {
            api("org.zeromq:jeromq:0.5.2")
            implementation(kotlin("reflect"))
            implementation(project(":communicator-api"))
        }
    }
}
