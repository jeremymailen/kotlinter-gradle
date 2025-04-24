package org.jmailen.gradle.kotlinter

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jmailen.gradle.kotlinter.pluginapplier.AndroidSourceSetApplier
import org.jmailen.gradle.kotlinter.pluginapplier.KotlinSourceSetApplier
import org.jmailen.gradle.kotlinter.support.reporterFileExtension
import org.jmailen.gradle.kotlinter.tasks.ConfigurableKtLintTask
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.InstallPrePushHookTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

class KotlinterPlugin : Plugin<Project> {
    companion object {
        const val KTLINT_CONFIGURATION_NAME = "ktlint"
    }

    private val extendablePlugins = mapOf(
        "org.jetbrains.kotlin.jvm" to KotlinSourceSetApplier,
        "org.jetbrains.kotlin.multiplatform" to KotlinSourceSetApplier,
        "org.jetbrains.kotlin.js" to KotlinSourceSetApplier,
        "kotlin-android" to AndroidSourceSetApplier,
    )

    override fun apply(project: Project) = with(project) {
        val kotlinterExtension = extensions.create("kotlinter", KotlinterExtension::class.java)
        val ktlintConfiguration = createKtLintConfiguration(kotlinterExtension)

        if (this == rootProject) {
            registerPrePushHookTask()
        }

        // Configure all tasks including custom user tasks regardless of which plugins are applied
        // This ensures that custom tasks work even without a Kotlin plugin
        tasks.withType(ConfigurableKtLintTask::class.java).configureEach { task ->
            task.ktlintClasspath.from(ktlintConfiguration)
        }

        // for known kotlin plugins, register tasks by convention.
        extendablePlugins.forEach { (pluginId, sourceResolver) ->
            pluginManager.withPlugin(pluginId) {
                val lintKotlin = registerParentLintTask()
                val formatKotlin = registerParentFormatTask()

                registerSourceSetTasks(kotlinterExtension, sourceResolver, lintKotlin, formatKotlin)
            }
        }
    }

    private fun Project.registerParentLintTask(): TaskProvider<Task> = tasks.register("lintKotlin") {
        it.group = "formatting"
        it.description = "Runs lint on the Kotlin source files."
    }.also { lintKotlin ->
        tasks.named("check").configure { check -> check.dependsOn(lintKotlin) }
    }

    private fun Project.registerParentFormatTask(): TaskProvider<Task> = tasks.register("formatKotlin") {
        it.group = "formatting"
        it.description = "Formats the Kotlin source files."
    }

    private fun Project.createKtLintConfiguration(kotlinterExtension: KotlinterExtension): Configuration {
        val configuration = configurations.maybeCreate(KTLINT_CONFIGURATION_NAME).apply {
            isCanBeResolved = true
            isCanBeConsumed = false
            isVisible = false

            // Use individual ktlint dependencies rather than the CLI to avoid variant selection issues
            val ktlintVersion = kotlinterExtension.ktlintVersion
            val deps = listOf(
                "com.pinterest.ktlint:ktlint-rule-engine:$ktlintVersion",
                "com.pinterest.ktlint:ktlint-ruleset-standard:$ktlintVersion",
                "com.pinterest.ktlint:ktlint-cli-reporter-core:$ktlintVersion",
                "com.pinterest.ktlint:ktlint-cli-reporter-plain:$ktlintVersion",
                "com.pinterest.ktlint:ktlint-cli-reporter-html:$ktlintVersion",
                "com.pinterest.ktlint:ktlint-cli-reporter-checkstyle:$ktlintVersion",
                "com.pinterest.ktlint:ktlint-cli-reporter-json:$ktlintVersion",
                "com.pinterest.ktlint:ktlint-cli-reporter-sarif:$ktlintVersion",
            )
            deps.forEach { dep ->
                dependencies.add(project.dependencies.create(dep))
            }
        }
        return configuration
    }

    private fun Project.registerSourceSetTasks(
        kotlinterExtension: KotlinterExtension,
        sourceResolver: SourceSetApplier,
        parentLintTask: TaskProvider<Task>,
        parentFormatTask: TaskProvider<Task>,
    ) {
        sourceResolver.applyToAll(this) { id, resolvedSources ->
            val lintSourceSetTask = tasks.register(
                "lintKotlin${id.replaceFirstChar(Char::titlecase)}",
                LintTask::class.java,
            ) { lintTask ->
                lintTask.source(resolvedSources)
                lintTask.ignoreLintFailures.set(provider { kotlinterExtension.ignoreLintFailures })
                lintTask.reports.set(
                    provider {
                        kotlinterExtension.reporters.associateWith { reporter ->
                            reportFile("$id-lint.${reporterFileExtension(reporter)}").get().asFile
                        }
                    },
                )
            }
            parentLintTask.configure { lintTask ->
                lintTask.dependsOn(lintSourceSetTask)
            }

            val formatSourceSetTask = tasks.register(
                "formatKotlin${id.replaceFirstChar(Char::titlecase)}",
                FormatTask::class.java,
            ) { formatTask ->
                formatTask.source(resolvedSources)
                formatTask.ignoreFormatFailures.set(provider { kotlinterExtension.ignoreFormatFailures })
                formatTask.ignoreLintFailures.set(provider { kotlinterExtension.ignoreLintFailures })
                formatTask.report.set(reportFile("$id-format.txt"))
            }
            parentFormatTask.configure { formatTask ->
                formatTask.dependsOn(formatSourceSetTask)
            }
        }
    }

    private fun Project.registerPrePushHookTask(): TaskProvider<InstallPrePushHookTask> =
        tasks.register("installKotlinterPrePushHook", InstallPrePushHookTask::class.java) {
            it.group = "build setup"
            it.description = "Installs Kotlinter Git pre-push hook"
        }
}

internal val String.id: String
    get() = split(" ").first()

internal fun Project.reportFile(name: String): Provider<RegularFile> = layout.buildDirectory.file("reports/ktlint/$name")
