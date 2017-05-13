package org.jmailen.gradle.kotlinter

import com.github.shyiko.ktlint.core.RuleSet
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jmailen.gradle.kotlinter.support.resolveRuleSets
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

class KotlinterPlugin : Plugin<Project> {

    override fun apply(project: Project?) {
        project?.let { p ->
            p.plugins.withType(KotlinPluginWrapper::class.java) {

                KotlinterApplier(p, p.kotlinSourceSets(), resolveRuleSets()).createTasks()
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

class KotlinterApplier(val project: Project, val kotlinSourceSets: List<SourceDirectorySet>, val ruleSets: List<RuleSet>) {

    fun createTasks() {
        val ruleSets = resolveRuleSets()

        val formatTasks = createFormatTasks(ruleSets)

        project.tasks.create("formatKotlin") {
            it.group = "formatting"
            it.description = "Formats the Kotlin source files."
            it.dependsOn(formatTasks)
        }

        val lintTasks = createLintTasks(ruleSets)

        val lintKotlin = project.tasks.create("lintKotlin") {
            it.group = "verification"
            it.description = "Runs lint on the Kotlin source files."
            it.dependsOn(lintTasks)
        }

        project.tasks.getByName("check") {
            it.dependsOn(lintKotlin)
        }
    }

    fun createFormatTasks(ruleSets: List<RuleSet>) =
            kotlinSourceSets.map { sourceSet ->
                val taskName = "formatKotlin${sourceSet.id().capitalize()}"

                project.tasks.create(taskName, FormatTask::class.java) { task ->
                    task.source(sourceSet.sourceDirectories.files)
                    task.ruleSets = ruleSets
                    task.report = reportFile("${sourceSet.id()}-format.txt")
                }
            }

    fun createLintTasks(ruleSets: List<RuleSet>) =
            kotlinSourceSets.map { sourceSet ->
                val taskName = "lintKotlin${sourceSet.id().capitalize()}"

                project.tasks.create(taskName, LintTask::class.java) { task ->
                    task.source(sourceSet.sourceDirectories.files)
                    task.ruleSets = ruleSets
                    task.report = reportFile("${sourceSet.id()}-lint.txt")
                }
            }

    private fun reportFile(name: String) = project.file("${project.buildDir}/reports/ktlint/$name")

    private fun SourceDirectorySet.id() = name.split(" ").first()
}
