import scientifik.useCoroutines

plugins {
   id("scientifik.mpp")
}

useCoroutines()

kotlin{
   sourceSets {
      commonMain {
         dependencies {
            implementation(project(":communicator-api"))
            implementation(project(":communicator-zmq"))
            implementation(project(":communicator-factories"))
         }
      }
      jvmMain {
         dependencies{
            implementation(project(":communicator-api"))
            implementation(project(":communicator-zmq"))
            implementation(project(":communicator-factories"))
         }
      }
   }
}