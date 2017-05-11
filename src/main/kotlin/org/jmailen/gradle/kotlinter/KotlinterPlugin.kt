package org.jmailen.gradle.kotlinter

import com.github.shyiko.ktlint.core.RuleSet
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jmailen.gradle.kotlinter.support.resolveRuleSets
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

class KotlinterPlugin : Plugin<Project> {

    override fun apply(project: Project?) {
        project?.let { p ->
            createTasks(p)
        }
    }

    fun createTasks(project: Project) {
        val ruleSets = resolveRuleSets()

        val formatTasks = createFormatTasks(project, ruleSets)

        project.tasks.create("formatKotlin") {
            it.group = "formatting"
            it.description = "Formats the Kotlin source files."
            it.dependsOn(formatTasks)
        }

        val lintTasks = createLintTasks(project, ruleSets)

        val lintKotlin = project.tasks.create("lintKotlin") {
            it.group = "verification"
            it.description = "Runs lint on the Kotlin source files."
            it.dependsOn(lintTasks)
        }

        project.tasks.getByName("check") {
            it.dependsOn(lintKotlin)
        }
    }

    fun createLintTasks(project: Project, ruleSets: List<RuleSet>) =
            project.kotlinSourceSets().map { sourceSet ->
                val taskName = "lintKotlin${sourceSet.id().capitalize()}"

                project.tasks.create(taskName, LintTask::class.java) { task ->
                    task.source(sourceSet.sourceDirectories.files)
                    task.ruleSets = ruleSets
                    task.report = project.reportFile("${sourceSet.id()}-lint.txt")
                }
            }

    fun createFormatTasks(project: Project, ruleSets: List<RuleSet>) =
            project.kotlinSourceSets().map { sourceSet ->
                val taskName = "formatKotlin${sourceSet.id().capitalize()}"

                project.tasks.create(taskName, FormatTask::class.java) { task ->
                    task.source(sourceSet.sourceDirectories.files)
                    task.ruleSets = ruleSets
                    task.report = project.reportFile("${sourceSet.id()}-format.txt")
                }
            }
}

fun Project.reportFile(name: String) = file("$buildDir/reports/ktlint/$name")

fun Project.kotlinSourceSets() = sourceSets().map { it.kotlin }.filterNotNull()

fun Project.sourceSets() = convention.getPlugin(JavaPluginConvention::class.java).sourceSets

fun SourceDirectorySet.id() = name.split(" ").first()

private val SourceSet.kotlin: SourceDirectorySet?
    get() = ((getConvention("kotlin") ?: getConvention("kotlin2js")) as? KotlinSourceSet)?.kotlin

internal fun Any.getConvention(name: String): Any? =
        (this as HasConvention).convention.plugins[name]
