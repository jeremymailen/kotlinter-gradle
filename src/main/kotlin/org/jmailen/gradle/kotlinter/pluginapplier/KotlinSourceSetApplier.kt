package org.jmailen.gradle.kotlinter.pluginapplier

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jmailen.gradle.kotlinter.SourceSetAction
import org.jmailen.gradle.kotlinter.SourceSetApplier
import org.jmailen.gradle.kotlinter.id

internal object KotlinSourceSetApplier : SourceSetApplier {
    override fun applyToAll(project: Project, action: SourceSetAction) {
        getSourceSets(project).configureEach { sourceSet ->
            sourceSet.kotlin.let { directorySet ->
                action(directorySet.name.id, project.provider { directorySet })
            }
        }
    }

    private fun getSourceSets(project: Project) = project.extensions.getByType(KotlinProjectExtension::class.java).sourceSets
}
