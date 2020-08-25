allprojects {
    group = "kscience.communicator"
    version = "0.0.1"
}

subprojects {
    repositories {
        jcenter()
        maven("https://dl.bintray.com/mipt-npm/dev")
    }
}
