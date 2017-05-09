package org.jmailen.gradle.kotlinter

import org.gradle.api.Plugin
import org.gradle.api.Project
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

        val lintKotlinMain = project.tasks.create("lintKotlinMain", LintTask::class.java) {
            it.setKotlinSource("main")
            it.ruleSets = ruleSets
        }
        val lintKotlinTest = project.tasks.create("lintKotlinTest", LintTask::class.java) {
            it.setKotlinSource("test")
            it.ruleSets = ruleSets
        }
        val lintKotlin = project.tasks.create("lintKotlin") {
            it.group = "verification"
            it.description = "Runs lint on the Kotlin source files."
            it.dependsOn(lintKotlinMain, lintKotlinTest)
        }

        project.tasks.getByName("check") {
            it.dependsOn(lintKotlin)
        }
    }
}

internal fun SourceTask.setKotlinSource(sourceSetName: String) {
    project.sourceSets().all { sourceSet ->
        if (sourceSet.name == sourceSetName) {
            sourceSet.kotlin?.srcDirs?.forEach { source(it) }
        }
    }
}

fun Project.sourceSets() = convention.getPlugin(JavaPluginConvention::class.java).sourceSets

private val SourceSet.kotlin: SourceDirectorySet?
    get() = ((getConvention("kotlin") ?: getConvention("kotlin2js")) as? KotlinSourceSet)?.kotlin

internal fun Any.getConvention(name: String): Any? =
        (this as HasConvention).convention.plugins[name]
