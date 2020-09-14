package org.jmailen.gradle.kotlinter.pluginapplier

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jmailen.gradle.kotlinter.SourceSetAction
import org.jmailen.gradle.kotlinter.SourceSetApplier
import org.jmailen.gradle.kotlinter.id

internal object KotlinJvmSourceSetApplier : SourceSetApplier {

    override fun applyToAll(project: Project, action: SourceSetAction) {
        getSourceSets(project).all { sourceSet ->
            sourceSet.kotlin.let { directorySet ->
                action(directorySet.name.id, project.provider { directorySet })
            }
        }
    }

    private fun getSourceSets(project: Project): NamedDomainObjectContainer<KotlinSourceSet> =
        project.extensions.findByType(KotlinJvmProjectExtension::class.java)!!.sourceSets
}
