import scientifik.useCoroutines

plugins {
    id("scientifik.mpp")
}

useCoroutines()

kotlin{
   sourceSets {
      val commonMain by getting {
         dependencies {
            api(project(":communicator-api"))
         }
      }
   }
}