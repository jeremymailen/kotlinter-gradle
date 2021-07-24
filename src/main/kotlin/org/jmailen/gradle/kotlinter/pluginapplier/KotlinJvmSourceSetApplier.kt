package org.jmailen.gradle.kotlinter.pluginapplier

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer
import org.jmailen.gradle.kotlinter.SourceSetAction
import org.jmailen.gradle.kotlinter.SourceSetApplier
import org.jmailen.gradle.kotlinter.id

internal object KotlinJvmSourceSetApplier : SourceSetApplier {

    override fun applyToAll(project: Project, action: SourceSetAction) {
        project.getSourceSets<KotlinJvmProjectExtension>().all { sourceSet ->
            sourceSet.kotlin.let { directorySet ->
                action(directorySet.name.id, project.provider { directorySet })
            }
        }
    }
}

internal inline fun <reified T : KotlinSourceSetContainer> Project.getSourceSets(): NamedDomainObjectContainer<KotlinSourceSet> =
    project.extensions.getByType(T::class.java).sourceSets
