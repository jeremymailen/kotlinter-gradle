package org.jmailen.gradle.kotlinter.pluginapplier

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.AndroidSourceSet
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.FileTree
import org.jmailen.gradle.kotlinter.SourceSetAction
import org.jmailen.gradle.kotlinter.SourceSetApplier
import org.jmailen.gradle.kotlinter.id

internal object AndroidSourceSetApplier : SourceSetApplier {

    override fun applyToAll(project: Project, action: SourceSetAction) {
        val android = project.extensions.findByName("android")
        (android as? BaseExtension)?.let {
            it.sourceSets.all { sourceSet ->
                val id = sourceSet.name.id
                action(id, project.provider { getKotlinFiles(project, sourceSet) })
            }
        }
    }

    private fun getKotlinFiles(project: Project, sourceSet: AndroidSourceSet) = sourceSet.java.srcDirs.map { dir ->
        project.fileTree(dir) { it.include("**/*.kt") }
    }.reduce { merged: FileTree, tree: ConfigurableFileTree ->
        merged + tree
    }
}
