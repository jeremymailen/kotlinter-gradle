@file:Suppress("deprecation") // https://issuetracker.google.com/issues/170650362

package org.jmailen.gradle.kotlinter.pluginapplier

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.AndroidSourceSet
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.jmailen.gradle.kotlinter.SourceSetAction
import org.jmailen.gradle.kotlinter.SourceSetApplier
import org.jmailen.gradle.kotlinter.id

internal object AndroidSourceSetApplier : SourceSetApplier {

    override fun applyToAll(project: Project, action: SourceSetAction) {
        val android = project.extensions.findByType(BaseExtension::class.java) ?: return
        android.sourceSets.configureEach { sourceSet ->
            val id = sourceSet.name.id
            action(id, project.provider { getKotlinFiles(project, sourceSet) })
        }
    }

    private fun getKotlinFiles(project: Project, sourceSet: AndroidSourceSet): FileTree? {
        val javaSources = sourceSet.java.srcDirs
        // detect Kotlin source paths supported by AGP 7 and later
        val kotlinSources = runCatching { (sourceSet.kotlin as? com.android.build.gradle.api.AndroidSourceDirectorySet)?.srcDirs }
            .getOrNull()
            .orEmpty()

        val emptyFileTree = project.files().asFileTree

        return (javaSources + kotlinSources)
            .map { dir -> project.fileTree(dir) { it.include("**/*.kt") } }
            .fold(emptyFileTree) { merged, tree -> merged + tree }
    }
}
