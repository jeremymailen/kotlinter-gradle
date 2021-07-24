package org.jmailen.gradle.kotlinter.pluginapplier

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jmailen.gradle.kotlinter.SourceSetAction
import org.jmailen.gradle.kotlinter.SourceSetApplier
import org.jmailen.gradle.kotlinter.id

internal object KotlinMultiplatformSourceSetApplier : SourceSetApplier {

    override fun applyToAll(project: Project, action: SourceSetAction) {
        project.getSourceSets<KotlinMultiplatformExtension>().all { sourceSet ->
            sourceSet.kotlin.let { directorySet ->
                action(directorySet.name.id, project.provider { directorySet })
            }
        }
    }
}
