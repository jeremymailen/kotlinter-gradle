package org.jmailen.gradle.kotlinter

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceTask
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jmailen.gradle.kotlinter.support.resolveRuleSets
import org.jmailen.gradle.kotlinter.tasks.LintTask

class KotlinterPlugin : Plugin<Project> {

    override fun apply(project: Project?) {
        project?.let { p ->
            createTasks(p)
        }
    }

    fun createTasks(project: Project) {
        val ruleSets = resolveRuleSets()

        val lintTasks = project.kotlinSourceSets().map { sourceSet ->
            val sourceSetId = sourceSet.name.split(" ").first().capitalize()

            project.tasks.create("lintKotlin$sourceSetId", LintTask::class.java) { task ->
                task.source(sourceSet)
                task.ruleSets = ruleSets
            }
        }

        val lintKotlin = project.tasks.create("lintKotlin") {
            it.group = "verification"
            it.description = "Runs lint on the Kotlin source files."
            it.dependsOn(lintTasks)
        }

        project.tasks.getByName("check") {
            it.dependsOn(lintKotlin)
        }
    }
}

fun Project.kotlinSourceSets() = sourceSets().map { it.kotlin }.filterNotNull()

fun Project.sourceSets() = convention.getPlugin(JavaPluginConvention::class.java).sourceSets

private val SourceSet.kotlin: SourceDirectorySet?
    get() = ((getConvention("kotlin") ?: getConvention("kotlin2js")) as? KotlinSourceSet)?.kotlin

internal fun Any.getConvention(name: String): Any? =
        (this as HasConvention).convention.plugins[name]
