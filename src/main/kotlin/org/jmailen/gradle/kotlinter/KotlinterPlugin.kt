package org.jmailen.gradle.kotlinter

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jmailen.gradle.kotlinter.support.KtLintParams
import org.jmailen.gradle.kotlinter.support.reporterFileExtension
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

class KotlinterPlugin : Plugin<Project> {

    val extendablePlugins = mapOf(
        "org.jetbrains.kotlin.jvm" to KotlinJvmSourceSetResolver,
        "kotlin-android" to AndroidSourceSetResolver
    )

    override fun apply(project: Project) = with(project) {
        val kotlinterExtension = extensions.create("kotlinter", KotlinterExtension::class.java)

        // for known kotlin plugins, create tasks by convention.
        extendablePlugins.forEach { (pluginId, sourceResolver) ->
            plugins.withId(pluginId) {

                val lintKotlin = registerParentLintTask()
                val formatKotlin = registerParentFormatTask()

                sourceResolver.applyToAll(project) { id, resolveSources ->
                    val ktLintParams = KtLintParams(
                        kotlinterExtension.indentSize,
                        kotlinterExtension.continuationIndentSize,
                        kotlinterExtension.experimentalRules,
                        kotlinterExtension.disabledRules,
                        editorConfigPath()
                    )

                    val lintKotlinPerVariant = tasks.register("lintKotlin${id.capitalize()}", LintTask::class.java) { lintTask ->
                        lintTask.source(resolveSources)
                        lintTask.ignoreFailures = kotlinterExtension.ignoreFailures
                        lintTask.reports = kotlinterExtension.reporters.associate { reporter ->
                            reporter to reportFile("$id-lint.${reporterFileExtension(reporter)}")
                        }
                        lintTask.ktLintParams = ktLintParams
                        lintTask.fileBatchSize = kotlinterExtension.fileBatchSize
                    }
                    lintKotlin.dependsOn(lintKotlinPerVariant)

                    val formatKotlinPerVariant = tasks.register("formatKotlin${id.capitalize()}", FormatTask::class.java) { formatTask ->
                        formatTask.source(resolveSources)
                        formatTask.report = reportFile("$id-format.txt")
                        formatTask.ktLintParams = ktLintParams
                        formatTask.fileBatchSize = kotlinterExtension.fileBatchSize
                    }
                    formatKotlin.dependsOn(formatKotlinPerVariant)
                }
            }
        }
    }

    private fun Project.registerParentLintTask() =
        tasks.register("lintKotlin") {
            it.group = "formatting"
            it.description = "Runs lint on the Kotlin source files."
        }.also {
            tasks.named("check").dependsOn(it)
        }

    private fun Project.registerParentFormatTask() =
        tasks.register("formatKotlin") {
            it.group = "formatting"
            it.description = "Formats the Kotlin source files."
        }
}

internal typealias SourceSetAction = (String, Provider<FileTree>) -> Unit

interface SourceSetResolver {
    fun applyToAll(project: Project, action: SourceSetAction)
}

internal object KotlinJvmSourceSetResolver : SourceSetResolver {

    override fun applyToAll(project: Project, action: SourceSetAction) {
        getSourceSets(project).all { sourceSet ->
            sourceSet.kotlin?.let { directorySet ->
                action(directorySet.name.id, project.provider { directorySet })
            }
        }
    }

    private fun getSourceSets(project: Project): SourceSetContainer =
        project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets

    private val SourceSet.kotlin: SourceDirectorySet?
        get() = ((getConvention("kotlin") ?: getConvention("kotlin2js")) as? KotlinSourceSet)?.kotlin

    private fun SourceSet.getConvention(name: String): Any? =
        (this as HasConvention).convention.plugins[name]
}

internal object AndroidSourceSetResolver : SourceSetResolver {

    override fun applyToAll(project: Project, action: SourceSetAction) {
        val android = project.extensions.findByName("android")
        (android as? BaseExtension)?.let {
            it.sourceSets.all { sourceSet ->
                val id = sourceSet.name.id
                action(id, project.provider { getKotlinFiles(project, sourceSet) })
            }
        }
    }

    private fun getKotlinFiles(project: Project, sourceSet: AndroidSourceSet) = sourceSet.java.srcDirs.map { dir ->
        project.fileTree(dir) { it.include("**/*.kt") }
    }.reduce { merged: FileTree, tree ->
        merged + tree
    }
}

internal val String.id
    get() = split(" ").first()

internal fun Project.reportFile(name: String) = file("$buildDir/reports/ktlint/$name")

internal fun Project.editorConfigPath() = with(rootProject.file(".editorconfig")) {
    if (this.exists()) this.path else null
}
