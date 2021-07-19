/*
 * Copyright 2018-2021 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package space.kscience.communicator.prettyapi.gradle

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.*

class CommunicatorGradleSubplugin : KotlinCompilerPluginSupportPlugin,
    @Suppress("DEPRECATION") // implementing to fix KT-39809
    KotlinGradleSubplugin<AbstractCompile> {
    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> = kotlinCompilation.target.project.provider { emptyList() }

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(GROUP_NAME, ARTIFACT_NAME)

    override fun getCompilerPluginId() = "space.kscience.communicator.prettyapi"

    //Stub implementation for legacy API, KT-39809
    override fun isApplicable(project: Project, task: AbstractCompile): Boolean = true

    override fun apply(
        project: Project,
        kotlinCompile: AbstractCompile,
        javaCompile: AbstractCompile?,
        variantData: Any?,
        androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation<KotlinCommonOptions>?,
    ): List<SubpluginOption> = throw GradleException(
        "This version of the kotlin-serialization Gradle plugin is built for a newer Kotlin version. " +
                "Please use an older version of kotlin-serialization or upgrade the Kotlin Gradle plugin version to make them match."
    )

    private companion object {
        private const val GROUP_NAME = "space.kscience.communicator.prettyapi"
        private const val ARTIFACT_NAME = "pretty-api-compiler-plugin"
    }
}
