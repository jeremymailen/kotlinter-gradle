package org.jmailen.gradle.kotlinter

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.jmailen.gradle.kotlinter.pluginapplier.AndroidSourceSetApplier
import org.jmailen.gradle.kotlinter.pluginapplier.KotlinJvmSourceSetApplier
import org.jmailen.gradle.kotlinter.pluginapplier.KotlinMultiplatformSourceSetApplier
import org.jmailen.gradle.kotlinter.support.reporterFileExtension
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.InstallPreCommitHookTask
import org.jmailen.gradle.kotlinter.tasks.InstallPrePushHookTask
import org.jmailen.gradle.kotlinter.tasks.LintTask
import java.io.File

class KotlinterPlugin : Plugin<Project> {

    private val extendablePlugins = mapOf(
        "org.jetbrains.kotlin.jvm" to KotlinJvmSourceSetApplier,
        "org.jetbrains.kotlin.multiplatform" to KotlinMultiplatformSourceSetApplier,
        "kotlin-android" to AndroidSourceSetApplier
    )

    override fun apply(project: Project) = with(project) {
        val kotlinterExtension = extensions.create("kotlinter", KotlinterExtension::class.java)

        if (this == rootProject) {
            registerPrePushHookTask()
        }

        // for known kotlin plugins, register tasks by convention.
        extendablePlugins.forEach { (pluginId, sourceResolver) ->
            pluginManager.withPlugin(pluginId) {

                val lintKotlin = registerParentLintTask()
                val formatKotlin = registerParentFormatTask()

                sourceResolver.applyToAll(this) { id, resolvedSources ->
                    val lintTaskPerSourceSet = tasks.register("lintKotlin${id.capitalize()}", LintTask::class.java) { lintTask ->
                        lintTask.source(resolvedSources)
                        lintTask.ignoreFailures.set(provider { kotlinterExtension.ignoreFailures })
                        lintTask.reports.set(
                            provider {
                                kotlinterExtension.reporters.associateWith { reporter ->
                                    reportFile("$id-lint.${reporterFileExtension(reporter)}")
                                }
                            }
                        )
                        lintTask.experimentalRules.set(provider { kotlinterExtension.experimentalRules })
                        lintTask.disabledRules.set(provider { kotlinterExtension.disabledRules.toList() })
                    }
                    lintKotlin.configure { lintTask ->
                        lintTask.dependsOn(lintTaskPerSourceSet)
                    }

                    val formatKotlinPerSourceSet = tasks.register("formatKotlin${id.capitalize()}", FormatTask::class.java) { formatTask ->
                        formatTask.source(resolvedSources)
                        formatTask.report.set(reportFile("$id-format.txt"))
                        formatTask.experimentalRules.set(provider { kotlinterExtension.experimentalRules })
                        formatTask.disabledRules.set(provider { kotlinterExtension.disabledRules.toList() })
                    }
                    formatKotlin.configure { formatTask ->
                        formatTask.dependsOn(formatKotlinPerSourceSet)
                    }
                }
            }
        }
    }

    private fun Project.registerParentLintTask(): TaskProvider<Task> =
        tasks.register("lintKotlin") {
            it.group = "formatting"
            it.description = "Runs lint on the Kotlin source files."
        }.also { lintKotlin ->
            tasks.named("check").configure { check -> check.dependsOn(lintKotlin) }
        }

    private fun Project.registerParentFormatTask(): TaskProvider<Task> =
        tasks.register("formatKotlin") {
            it.group = "formatting"
            it.description = "Formats the Kotlin source files."
        }

    private fun Project.registerPrePushHookTask(): TaskProvider<InstallPrePushHookTask> =
        tasks.register("installKotlinterPrePushHook", InstallPrePushHookTask::class.java) {
            it.group = "build setup"
            it.description = "Installs Kotlinter Git pre-push hook"
        }

    private fun Project.registerPreCommitHookTask(): TaskProvider<InstallPreCommitHookTask> =
        tasks.register("installKotlinterPreCommitHook", InstallPreCommitHookTask::class.java) {
            it.group = "build setup"
            it.description = "Installs Kotlinter Git pre-commit hook"
        }
}

internal val String.id: String
    get() = split(" ").first()

internal fun Project.reportFile(name: String): File = file("$buildDir/reports/ktlint/$name")
