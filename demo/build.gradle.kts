import scientifik.useCoroutines

plugins { id("scientifik.mpp") }

useCoroutines()

kotlin.sourceSets {
    all { languageSettings.useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts") }

    commonMain {
        dependencies {
            api(project(":communicator-api"))
            api(project(":communicator-zmq"))
            api(project(":communicator-transport"))
            api("io.github.microutils:kotlin-logging-common:1.8.3")
        }
    }

    jsMain { dependencies { api("io.github.microutils:kotlin-logging-js:1.8.3") } }

    jvmMain {
        dependencies {
            api("io.github.microutils:kotlin-logging:1.8.3")
            api("org.slf4j:slf4j-simple:1.7.30")
            api("org.zeromq:jeromq:0.5.2")
        }
    }

}