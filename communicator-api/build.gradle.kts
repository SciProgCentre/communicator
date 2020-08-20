import scientifik.useCoroutines

plugins { id("scientifik.mpp") }
useCoroutines()

dependencies {
    val serializationVersion = "0.20.0"
    commonMainApi("org.jetbrains.kotlinx:kotlinx-io:0.2.0-npm-dev-8")
    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serializationVersion")
    jvmMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationVersion")
    jsMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$serializationVersion")
}
