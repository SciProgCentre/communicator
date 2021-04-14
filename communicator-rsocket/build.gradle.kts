internal val kotlinLoggingVersion: String by project


plugins { kotlin("multiplatform") }

kotlin {
    explicitApi()
    jvm()

    sourceSets {
        val jvmMain by getting {
            repositories {
                jcenter()
            }

            dependencies {
                api("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
                implementation("io.rsocket.kotlin:rsocket-core:0.12.0")
                implementation("io.rsocket.kotlin:rsocket-transport-ktor:0.12.0")
                implementation("io.rsocket.kotlin:rsocket-transport-ktor-client:0.12.0")
                implementation("io.rsocket.kotlin:rsocket-transport-ktor-server:0.12.0")
                implementation("io.ktor:ktor-client-cio:1.4.3")
                implementation("io.ktor:ktor-server-cio:1.4.3")
                api(project(":communicator-api"))
                api(project(":communicator-zmq"))
            }
        }
    }
}
