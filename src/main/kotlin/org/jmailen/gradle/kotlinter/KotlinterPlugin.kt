package org.jmailen.gradle.kotlinter

import com.android.build.gradle.BaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jmailen.gradle.kotlinter.support.reporterFileExtension
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

class KotlinterPlugin : Plugin<Project> {

    val extendablePlugins = mapOf(
            "org.jetbrains.kotlin.jvm" to this::kotlinSourceSets,
            "kotlin-android" to this::androidSourceSets)

    override fun apply(project: Project) {
        val kotlinterExtention = project.extensions.create("kotlinter", KotlinterExtension::class.java)

        // for known kotlin plugins, create tasks by convention.
        extendablePlugins.forEach { pluginId, sourceResolver ->
            project.plugins.withId(pluginId) {
                val sourceSets = sourceResolver(project)
                KotlinterApplier(project).createTasks(sourceSets)
            }
        }

        project.afterEvaluate {
            project.tasks.withType(LintTask::class.java) { lintTask ->
                lintTask.ignoreFailures = kotlinterExtention.ignoreFailures
                lintTask.indentSize = kotlinterExtention.indentSize
                lintTask.continuationIndentSize = kotlinterExtention.continuationIndentSize
                lintTask.reports = kotlinterExtention.reporters().associate { reporter ->
                    reporter to project.reportFile("${lintTask.sourceSetId}-lint.${reporterFileExtension(reporter)}")
                }
            }
            project.tasks.withType(FormatTask::class.java) { formatTask ->
                formatTask.indentSize = kotlinterExtention.indentSize
                formatTask.continuationIndentSize = kotlinterExtention.continuationIndentSize
            }
        }
    }

    private fun kotlinSourceSets(project: Project): List<SourceSetInfo> {
        return project.sourceSets().map { it.kotlin }.filterNotNull().map {
            SourceSetInfo(it.name, it.sourceDirectories)
        }
    }

    private fun androidSourceSets(project: Project): List<SourceSetInfo> {
        val android = project.extensions.findByName("android")
        val sourceSetInfos = (android as? BaseExtension)?.let {
            it.sourceSets.map { androidSourceSet ->

                val kotlinSourceTree = androidSourceSet.java.srcDirs.map { dir ->
                    project.fileTree(dir) {
                        it.include("**/*.kt")
                    }
                }.reduce { merged: FileTree, tree ->
                    merged.plus(tree)
                }

                SourceSetInfo(androidSourceSet.name, kotlinSourceTree)
            }
        }
        return sourceSetInfos ?: emptyList()
    }

    private fun Project.sourceSets() = convention.getPlugin(JavaPluginConvention::class.java).sourceSets

    private val SourceSet.kotlin: SourceDirectorySet?
        get() = ((getConvention("kotlin") ?: getConvention("kotlin2js")) as? KotlinSourceSet)?.kotlin

    private fun Any.getConvention(name: String): Any? =
            (this as HasConvention).convention.plugins[name]
}

class KotlinterApplier(val project: Project) {

    fun createTasks(kotlinSourceSets: List<SourceSetInfo>) {

        val formatTasks = kotlinSourceSets.map { createFormatTask(it) }

        project.tasks.create("formatKotlin") {
            it.group = "formatting"
            it.description = "Formats the Kotlin source files."
            it.dependsOn(formatTasks)
        }

        val lintTasks = kotlinSourceSets.map { createLintTask(it) }

        val lintKotlin = project.tasks.create("lintKotlin") {
            it.group = "formatting"
            it.description = "Runs lint on the Kotlin source files."
            it.dependsOn(lintTasks)
        }

        project.tasks.getByName("check") {
            it.dependsOn(lintKotlin)
        }
    }

    fun createFormatTask(sourceSet: SourceSetInfo) =
            project.tasks.create("formatKotlin${sourceSet.id().capitalize()}", FormatTask::class.java) {
                it.source(sourceSet.files())
                it.report = project.reportFile("${sourceSet.id()}-format.txt")
            }

    fun createLintTask(sourceSet: SourceSetInfo) =
            project.tasks.create("lintKotlin${sourceSet.id().capitalize()}", LintTask::class.java) {
                it.source(sourceSet.files())
                it.sourceSetId = sourceSet.id()
            }
}

class SourceSetInfo(val name: String, val sourceDirectories: FileCollection) {

    fun id() = name.split(" ").first()

    fun files() = sourceDirectories.files
}

fun Project.reportFile(name: String) = file("${project.buildDir}/reports/ktlint/$name")
