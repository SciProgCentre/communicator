import scientifik.useCoroutines

plugins { id("scientifik.mpp") }
useCoroutines()

dependencies {
   val serialization_version = "0.20.0"
   commonMainImplementation("org.jetbrains.kotlinx:kotlinx-io:0.2.0-npm-dev-8")
   commonMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serialization_version")
   jvmMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serialization_version")
   jsMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$serialization_version")
}
