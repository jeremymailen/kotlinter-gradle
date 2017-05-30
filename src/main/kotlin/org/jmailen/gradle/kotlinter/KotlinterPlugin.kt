package org.jmailen.gradle.kotlinter

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

class KotlinterPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val kotlinterExtention = project.extensions.create("kotlinter", KotlinterExtension::class.java)

        // for known kotlin plugins, create tasks by convention.
        project.plugins.withId("org.jetbrains.kotlin.jvm") {
            KotlinterApplier(project).createTasks(project.kotlinSourceSets())
        }

        project.afterEvaluate {
            project.tasks.withType(LintTask::class.java) { lintTask ->
                lintTask.ignoreFailures = kotlinterExtention.ignoreFailures
            }
        }
    }

    private fun Project.kotlinSourceSets() = sourceSets().map { it.kotlin }.filterNotNull()

    private fun Project.sourceSets() = convention.getPlugin(JavaPluginConvention::class.java).sourceSets

    private val SourceSet.kotlin: SourceDirectorySet?
        get() = ((getConvention("kotlin") ?: getConvention("kotlin2js")) as? KotlinSourceSet)?.kotlin

    private fun Any.getConvention(name: String): Any? =
            (this as HasConvention).convention.plugins[name]
}

class KotlinterApplier(val project: Project) {

    fun createTasks(kotlinSourceSets: List<SourceDirectorySet>) {
        val formatTasks = kotlinSourceSets.map { createFormatTask(it) }

        project.tasks.create("formatKotlin") {
            it.group = "formatting"
            it.description = "Formats the Kotlin source files."
            it.dependsOn(formatTasks)
        }

        val lintTasks = kotlinSourceSets.map { createLintTask(it) }

        val lintKotlin = project.tasks.create("lintKotlin") {
            it.group = "verification"
            it.description = "Runs lint on the Kotlin source files."
            it.dependsOn(lintTasks)
        }

        project.tasks.getByName("check") {
            it.dependsOn(lintKotlin)
        }
    }

    fun createFormatTask(sourceSet: SourceDirectorySet) =
            project.tasks.create("formatKotlin${sourceSet.id().capitalize()}", FormatTask::class.java) {
                it.source(sourceSet.sourceDirectories.files)
                it.report = reportFile("${sourceSet.id()}-format.txt")
            }

    fun createLintTask(sourceSet: SourceDirectorySet) =
            project.tasks.create("lintKotlin${sourceSet.id().capitalize()}", LintTask::class.java) { task ->
                task.source(sourceSet.sourceDirectories.files)
                task.report = reportFile("${sourceSet.id()}-lint.txt")
            }

    private fun reportFile(name: String) = project.file("${project.buildDir}/reports/ktlint/$name")

    private fun SourceDirectorySet.id() = name.split(" ").first()
}
