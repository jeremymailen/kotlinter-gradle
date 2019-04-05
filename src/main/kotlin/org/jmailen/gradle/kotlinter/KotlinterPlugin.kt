package org.jmailen.gradle.kotlinter

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.AndroidSourceSet
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jmailen.gradle.kotlinter.support.reporterFileExtension
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask
import java.io.File

class KotlinterPlugin : Plugin<Project> {

    val extendablePlugins = mapOf(
            "org.jetbrains.kotlin.jvm" to KotlinJvmSourceSetResolver,
            "kotlin-android" to AndroidSourceSetResolver)

    override fun apply(project: Project) {
        val kotlinterExtension = project.extensions.create("kotlinter", KotlinterExtension::class.java)

        // for known kotlin plugins, create tasks by convention.
        val taskCreator = TaskCreator(project)
        extendablePlugins.forEach { pluginId, sourceResolver ->
            project.plugins.withId(pluginId) {

                sourceResolver.applyToAll(project) { id, files ->
                    taskCreator.createSourceSetTasks(id, files)
                }

                taskCreator.createParentTasks()
            }
        }

        project.afterEvaluate {
            taskCreator.lintTasks.forEach { lintTask ->
                lintTask.ignoreFailures = kotlinterExtension.ignoreFailures
                lintTask.indentSize = kotlinterExtension.indentSize
                lintTask.continuationIndentSize = kotlinterExtension.continuationIndentSize
                lintTask.reports = kotlinterExtension.reporters().associate { reporter ->
                    reporter to project.reportFile("${lintTask.sourceSetId}-lint.${reporterFileExtension(reporter)}")
                }
                lintTask.experimentalRules = kotlinterExtension.experimentalRules
                lintTask.fileBatchSize = kotlinterExtension.fileBatchSize ?: KotlinterExtension.DEFAULT_LINT_FILE_BATCH_SIZE
            }
            taskCreator.formatTasks.forEach { formatTask ->
                formatTask.indentSize = kotlinterExtension.indentSize
                formatTask.continuationIndentSize = kotlinterExtension.continuationIndentSize
                formatTask.experimentalRules = kotlinterExtension.experimentalRules
                formatTask.fileBatchSize = kotlinterExtension.fileBatchSize ?: KotlinterExtension.DEFAULT_FORMAT_FILE_BATCH_SIZE
            }
        }
    }
}

class TaskCreator(private val project: Project) {

    val formatTasks = mutableListOf<FormatTask>()
    val lintTasks = mutableListOf<LintTask>()

    fun createSourceSetTasks(id: String, files: Set<File>) {

        formatTasks += project.tasks.create("formatKotlin${id.capitalize()}", FormatTask::class.java) {
            it.source(files)
            it.report = project.reportFile("$id-format.txt")
        }

        lintTasks += project.tasks.create("lintKotlin${id.capitalize()}", LintTask::class.java) {
            it.source(files)
            it.sourceSetId = id
        }
    }

    fun createParentTasks() {
        project.tasks.create("formatKotlin") {
            it.group = "formatting"
            it.description = "Formats the Kotlin source files."
            it.dependsOn(formatTasks)
        }

        val lintKotlin = project.tasks.create("lintKotlin") {
            it.group = "formatting"
            it.description = "Runs lint on the Kotlin source files."
            it.dependsOn(lintTasks)
        }

        project.tasks.getByName("check") {
            it.dependsOn(lintKotlin)
        }
    }
}

typealias SourceSetAction = (String, Set<File>) -> Unit

interface SourceSetResolver {
    fun applyToAll(project: Project, action: SourceSetAction)
}

object KotlinJvmSourceSetResolver : SourceSetResolver {

    override fun applyToAll(project: Project, action: SourceSetAction) {
        getSourceSets(project).all { sourceSet ->
            getKotlinFiles(sourceSet)?.let { files ->
                action(getSourceSetName(sourceSet)!!.id, files)
            }
        }
    }

    private fun getSourceSets(project: Project): SourceSetContainer =
        project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets

    private fun getSourceSetName(sourceSet: SourceSet) = sourceSet.kotlin?.name

    private fun getKotlinFiles(sourceSet: SourceSet) = sourceSet.kotlin?.files

    private val SourceSet.kotlin: SourceDirectorySet?
        get() = ((getConvention("kotlin") ?: getConvention("kotlin2js")) as? KotlinSourceSet)?.kotlin

    private fun SourceSet.getConvention(name: String): Any? =
        (this as HasConvention).convention.plugins[name]
}

object AndroidSourceSetResolver : SourceSetResolver {

    override fun applyToAll(project: Project, action: SourceSetAction) {
        val android = project.extensions.findByName("android")
        (android as? BaseExtension)?.let {
            it.sourceSets.all { sourceSet ->
                val id = sourceSet.name.id
                val files = getKotlinFiles(project, sourceSet)
                if (files.isNotEmpty()) {
                    action(id, files)
                }
            }
        }
    }

    private fun getKotlinFiles(project: Project, sourceSet: AndroidSourceSet) = sourceSet.java.srcDirs.map { dir ->
        project.fileTree(dir) {
            it.include("**/*.kt")
        }
    }.reduce { merged: FileTree, tree ->
        merged.plus(tree)
    }.files
}

val String.id
    get() = split(" ").first()

fun Project.reportFile(name: String) = file("${project.buildDir}/reports/ktlint/$name")
