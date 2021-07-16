import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import ru.mipt.npm.gradle.Maturity
import java.net.URL

plugins {
    id("ru.mipt.npm.gradle.project")
    kotlin("multiplatform") apply false
}

allprojects {
    group = "space.kscience"
    version = "0.0.1"

    repositories {
        mavenCentral()
        mavenLocal()
    }
}

subprojects {
    val p = this@subprojects
    if (p.name != "demo")
    apply<DokkaPlugin>()
    tasks.withType<DokkaTask> {
        dokkaSourceSets.configureEach {
            val readmeFile = File(this@subprojects.projectDir, "./README.md")
            if (readmeFile.exists()) includes.from(readmeFile.absolutePath)

            sourceLink {
                localDirectory.set(file("${p.name}/src/main/kotlin"))

                remoteUrl.set(
                    URL("https://github.com/mipt-npm/${rootProject.name}/tree/master/${p.name}/src/main/kotlin/")
                )
            }
        }
    }

    readme {
        maturity = Maturity.PROTOTYPE
    }
}

ksciencePublish {
    vcs("https://github.com/mipt-npm/communicator")
    space(publish = true)
}
