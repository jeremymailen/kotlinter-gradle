package org.jmailen.gradle.kotlinter

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

internal typealias SourceSetAction = (String, Provider<FileTree>) -> Unit

interface SourceSetApplier {
    fun applyToAll(project: Project, action: SourceSetAction)

    val Project.kotlinSourceSets: NamedDomainObjectContainer<KotlinSourceSet>
        get() = project.extensions.getByType(KotlinProjectExtension::class.java).sourceSets
}
