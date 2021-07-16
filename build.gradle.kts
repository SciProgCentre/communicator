import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.DokkaTaskPartial
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
    if (p.name != "demo") apply<DokkaPlugin>()

    afterEvaluate {
        tasks.withType<DokkaTaskPartial> {
            dependsOn(tasks["assemble"])

            dokkaSourceSets.configureEach {
                val readmeFile = p.projectDir.resolve("README.md")
                if (readmeFile.exists()) includes.from(readmeFile)
                val kotlinDirPath = "src/$name/kotlin"
                val kotlinDir = file(kotlinDirPath)

                if (kotlinDir.exists()) sourceLink {
                    localDirectory.set(kotlinDir)

                    remoteUrl.set(
                        URL("https://github.com/mipt-npm/${rootProject.name}/tree/master/${p.name}/$kotlinDirPath")
                    )
                }

                externalDocumentationLink(
                    "https://api.ktor.io/ktor-io/",
                    "https://api.ktor.io/ktor-io/ktor-io/package-list",
                )
            }
        }
    }

    readme {
        maturity = Maturity.PROTOTYPE
    }
}

ksciencePublish {
    vcs("https://github.com/mipt-npm/${rootProject.name}")
    space(publish = true)
}
