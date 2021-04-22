internal val kotlinLoggingVersion: String by project


plugins {
    kotlin("multiplatform")
}

kotlin {
    explicitApi()
    jvm()
    js {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":communicator-api"))

                api("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")

                implementation("io.rsocket.kotlin:rsocket-core:0.12.0")
                implementation("io.rsocket.kotlin:rsocket-transport-ktor:0.12.0")
                implementation("io.rsocket.kotlin:rsocket-transport-ktor-client:0.12.0")
            }
        }

        val jvmMain by getting {
            repositories {
                jcenter()
            }
            dependencies {
                implementation(project(":communicator-zmq"))
                api(project(":communicator-transport"))
                implementation("io.ktor:ktor-server-cio:1.4.3")
                implementation("io.ktor:ktor-client-cio:1.4.3")
                implementation("io.rsocket.kotlin:rsocket-transport-ktor-server:0.12.0")
            }
        }

        val jsMain by getting {
            repositories {
                jcenter()
            }

            dependencies {
                implementation("io.ktor:ktor-client-js:1.4.3")
            }
        }
    }
}
println("-----names:")
sourceSets.forEach{println(it)}
val runJvmServer by tasks.creating(JavaExec::class) {
    group = "application"
    main = "space.kscience.communicator-rsocket.ExampleServerKt"
}

//val runJvmProxy by tasks.creating(JavaExec::class) {
//    group = "application"
////    classpath = sourceSets["main"].runtimeClasspath
//    main = "space.kscience.communicator.rsocket.ExampleServerKt"
//}

//val runJvmServer by tasks.creating(Exec) {
//    commandLine("")
//}


