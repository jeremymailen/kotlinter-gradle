package org.jmailen.gradle.kotlinter

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.GradleVersion
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
        "com.android.base" to AndroidSourceSetApplier,
    )

    override fun apply(project: Project) = with(project) {
        if (GradleVersion.current() < GradleVersion.version("9.0.0")) {
            throw GradleException("Kotlinter requires Gradle 9.0 or newer to run KtLint 2.x")
        }
        val kotlinterExtension = extensions.create("kotlinter", KotlinterExtension::class.java)
        val ktlintConfiguration = configurations.maybeCreate(KTLINT_CONFIGURATION_NAME).apply {
            isCanBeResolved = true
            isCanBeConsumed = false
            attributes {
                it.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
                it.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class.java, Category.LIBRARY))
                it.attribute(
                    TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                    objects.named(TargetJvmEnvironment::class.java, TargetJvmEnvironment.STANDARD_JVM),
                )
            }
        }

        afterEvaluate {
            // kotlinterExtension settings are now resolved
            it.configureKtLintConfiguration(ktlintConfiguration, kotlinterExtension.ktlintVersion)

            if (this == rootProject) {
                registerPrePushHookTask()
            }

            // for known kotlin plugins, register tasks by convention.
            extendablePlugins.forEach { (pluginId, sourceResolver) ->
                pluginManager.withPlugin(pluginId) {
                    val lintKotlin = registerParentLintTask()
                    val formatKotlin = registerParentFormatTask()

                    registerSourceSetTasks(kotlinterExtension, sourceResolver, lintKotlin, formatKotlin)
                }
            }

            // Configure all tasks including custom user tasks regardless of which plugins are applied
            // This ensures that custom tasks work even without a Kotlin plugin
            tasks.withType(ConfigurableKtLintTask::class.java).configureEach { task ->
                task.ktlintClasspath.from(ktlintConfiguration)
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

    private fun Project.configureKtLintConfiguration(ktlintConfiguration: Configuration, ktlintVersion: String) {
        if (!ktlintVersion.contains('+') && !ktlintVersion.startsWith("latest.") &&
            !ktlintVersion.startsWith('[') && !ktlintVersion.startsWith('(') &&
            !isSupportedKtLintVersion(ktlintVersion)
        ) {
            throw GradleException("Kotlinter requires KtLint 2.x starting with 2.0.0-ALPHA-5; requested $ktlintVersion")
        }
        // Use individual ktlint dependencies rather than the CLI to avoid variant selection issues
        val deps = listOf(
            "io.github.ktlint.core:ktlint-rule-engine:$ktlintVersion",
            "io.github.ktlint.core:ktlint-ruleset-standard:$ktlintVersion",
            "io.github.ktlint.core:ktlint-cli-reporter-core:$ktlintVersion",
            "io.github.ktlint.core:ktlint-cli-reporter-plain:$ktlintVersion",
            "io.github.ktlint.core:ktlint-cli-reporter-html:$ktlintVersion",
            "io.github.ktlint.core:ktlint-cli-reporter-checkstyle:$ktlintVersion",
            "io.github.ktlint.core:ktlint-cli-reporter-json:$ktlintVersion",
            "io.github.ktlint.core:ktlint-cli-reporter-sarif:$ktlintVersion",
            "io.github.ktlint.core:ktlint-com-pinterest-backward-compatibility:$ktlintVersion",
        )
        deps.forEach { dep ->
            ktlintConfiguration.dependencies.add(project.dependencies.create(dep))
        }
        listOf(
            "ktlint-rule-engine",
            "ktlint-rule-engine-core",
            "ktlint-cli-ruleset-core",
            "ktlint-ruleset-standard",
            "ktlint-cli-reporter-core",
        ).forEach { module ->
            ktlintConfiguration.exclude(mapOf("group" to "com.pinterest.ktlint", "module" to module))
        }
        ktlintConfiguration.resolutionStrategy.componentSelection.all { selection ->
            if (selection.candidate.group == "io.github.ktlint.core" &&
                selection.candidate.module.startsWith("ktlint-") &&
                !isSupportedKtLintVersion(selection.candidate.version)
            ) {
                selection.reject("Kotlinter requires KtLint 2.x starting with 2.0.0-ALPHA-5")
            }
        }
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

internal fun isSupportedKtLintVersion(version: String): Boolean {
    val match = Regex("""^(\d+)\.(\d+)\.(\d+)(?:-([A-Za-z]+)-?(\d+)?)?""").matchEntire(version)
        ?: return false
    val (major, minor, patch, qualifier, qualifierNumber) = match.destructured
    if (major.toIntOrNull() != 2) return false
    if (minor.toIntOrNull() == null || patch.toIntOrNull() == null) return false
    if (minor.toInt() > 0 || patch.toInt() > 0 || qualifier.isEmpty()) return true
    return when (qualifier.lowercase()) {
        "alpha" -> (qualifierNumber.toIntOrNull() ?: return false) >= 5
        "beta", "rc" -> true
        else -> false
    }
}

internal val String.id: String
    get() = split(" ").first()

internal fun Project.reportFile(name: String): Provider<RegularFile> = layout.buildDirectory.file("reports/ktlint/$name")
