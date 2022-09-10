package org.jmailen.gradle.kotlinter

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskProvider
import org.jmailen.gradle.kotlinter.pluginapplier.AndroidSourceSetApplier
import org.jmailen.gradle.kotlinter.pluginapplier.KotlinSourceSetApplier
import org.jmailen.gradle.kotlinter.support.reporterFileExtension
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.InstallPreCommitHookTask
import org.jmailen.gradle.kotlinter.tasks.InstallPrePushHookTask
import org.jmailen.gradle.kotlinter.tasks.LintTask
import java.io.File

class KotlinterPlugin : Plugin<Project> {

    companion object {
        private const val KTLINT_CONFIGURATION_NAME = "ktlint"
        private const val RULE_SET_CONFIGURATION_NAME = "ktlintRuleSet"
    }

    private val extendablePlugins = mapOf(
        "org.jetbrains.kotlin.jvm" to KotlinSourceSetApplier,
        "org.jetbrains.kotlin.multiplatform" to KotlinSourceSetApplier,
        "org.jetbrains.kotlin.js" to KotlinSourceSetApplier,
        "kotlin-android" to AndroidSourceSetApplier,
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

                val ktlintConfiguration = createKtlintConfiguration(kotlinterExtension)
                val ruleSetConfiguration = createRuleSetConfiguration(ktlintConfiguration)

                sourceResolver.applyToAll(this) { id, resolvedSources ->
                    val lintTaskPerSourceSet = tasks.register("lintKotlin${id.capitalize()}", LintTask::class.java) { lintTask ->
                        lintTask.source(resolvedSources)
                        lintTask.ignoreFailures.set(provider { kotlinterExtension.ignoreFailures })
                        lintTask.reports.set(
                            provider {
                                kotlinterExtension.reporters.associateWith { reporter ->
                                    reportFile("$id-lint.${reporterFileExtension(reporter)}")
                                }
                            },
                        )
                        lintTask.experimentalRules.set(provider { kotlinterExtension.experimentalRules })
                        lintTask.disabledRules.set(provider { kotlinterExtension.disabledRules.toList() })
                        lintTask.ktlintClasspath.setFrom(ktlintConfiguration)
                        lintTask.ruleSetsClasspath.setFrom(ruleSetConfiguration)
                    }
                    lintKotlin.configure { lintTask ->
                        lintTask.dependsOn(lintTaskPerSourceSet)
                    }

                    val formatKotlinPerSourceSet = tasks.register("formatKotlin${id.capitalize()}", FormatTask::class.java) { formatTask ->
                        formatTask.source(resolvedSources)
                        formatTask.report.set(reportFile("$id-format.txt"))
                        formatTask.experimentalRules.set(provider { kotlinterExtension.experimentalRules })
                        formatTask.disabledRules.set(provider { kotlinterExtension.disabledRules.toList() })
                        formatTask.ktlintClasspath.setFrom(ktlintConfiguration)
                        formatTask.ruleSetsClasspath.setFrom(ruleSetConfiguration)
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

    private fun Project.createKtlintConfiguration(kotlinterExtension: KotlinterExtension): Configuration =
        configurations.maybeCreate(KTLINT_CONFIGURATION_NAME).apply {
            isCanBeResolved = true
            isCanBeConsumed = false
            isVisible = false

            val dependencyProvider = provider {
                val ktlintVersion = kotlinterExtension.ktlintVersion
                this@createKtlintConfiguration.dependencies.create("com.pinterest:ktlint:$ktlintVersion")
            }

            dependencies.addLater(dependencyProvider)
        }

    @Suppress("UnstableApiUsage")
    private fun Project.createRuleSetConfiguration(
        ktlintConfiguration: Configuration,
    ): Configuration = configurations.maybeCreate(RULE_SET_CONFIGURATION_NAME).apply {
        isCanBeResolved = true
        isCanBeConsumed = false
        isVisible = false

        shouldResolveConsistentlyWith(ktlintConfiguration)
    }
}

internal val String.id: String
    get() = split(" ").first()

internal fun Project.reportFile(name: String): File = file("$buildDir/reports/ktlint/$name")
