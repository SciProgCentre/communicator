plugins { kotlin("multiplatform") apply false }

allprojects {
    group = "space.kscience.communicator"
    version = "0.0.1"

    repositories {
        mavenCentral()
        mavenLocal()
    }
}
