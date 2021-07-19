internal val asmVersion: String by project
internal val junitVersion: String by project
internal val slf4jVersion: String by project

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

kotlin {
    explicitApi()

    sourceSets.all {
        with(languageSettings) {
            useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
            useExperimentalAnnotation("kotlinx.serialization.InternalSerializationApi")
        }
    }
}

dependencies {
    api(project(":communicator-api"))
    implementation(kotlin("reflect"))
    implementation("org.ow2.asm:asm-commons:9.2")
    testImplementation("org.slf4j:slf4j-simple:$slf4jVersion")
    testImplementation(project(":communicator-zmq"))
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
}

tasks.withType<Test> {
    useJUnitPlatform()

    systemProperty(
        "space.kscience.communicator.prettyapi.dump.generated.classes",
        System.getProperty("space.kscience.communicator.prettyapi.dump.generated.classes"),
    )
}
