package org.jmailen.gradle.kotlinter.tasks

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceTask
import org.jmailen.gradle.kotlinter.KotlinterExtension.Companion.DEFAULT_CONTINUATION_INDENT_SIZE
import org.jmailen.gradle.kotlinter.KotlinterExtension.Companion.DEFAULT_DISABLED_RULES
import org.jmailen.gradle.kotlinter.KotlinterExtension.Companion.DEFAULT_EXPERIMENTAL_RULES
import org.jmailen.gradle.kotlinter.KotlinterExtension.Companion.DEFAULT_FILE_BATCH_SIZE
import org.jmailen.gradle.kotlinter.KotlinterExtension.Companion.DEFAULT_INDENT_SIZE
import org.jmailen.gradle.kotlinter.support.KtLintParams

abstract class ConfigurableKtLintTask : SourceTask() {

    @Input
    val fileBatchSize = property(default = DEFAULT_FILE_BATCH_SIZE)

    @Input
    val indentSize = property(default = DEFAULT_INDENT_SIZE)
    @Input
    val continuationIndentSize = property(default = DEFAULT_CONTINUATION_INDENT_SIZE)
    @Input
    val experimentalRules = property(default = DEFAULT_EXPERIMENTAL_RULES)
    @Input
    val disabledRules = listProperty(default = DEFAULT_DISABLED_RULES.toList())
    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    val editorConfigPath = project.objects.fileProperty()

    @Internal
    protected fun getKtLintParams() = KtLintParams(
        indentSize = indentSize.get(),
        continuationIndentSize = continuationIndentSize.get(),
        experimentalRules = experimentalRules.get(),
        disabledRules = disabledRules.get(),
        editorConfigPath = editorConfigPath.asFile.orNull?.path
    )

    @Internal
    protected fun getChunkedSource(): List<List<File>> =
        source.chunked(fileBatchSize.get())
}

internal inline fun <reified T> DefaultTask.property(default: T? = null) =
    project.objects.property(T::class.java).apply {
        set(default)
    }

internal inline fun <reified T> DefaultTask.listProperty(default: Iterable<T> = emptyList()) =
    project.objects.listProperty(T::class.java).apply {
        set(default)
    }

internal inline fun <reified K, reified V> DefaultTask.mapProperty(default: Map<K, V> = emptyMap()) =
    project.objects.mapProperty(K::class.java, V::class.java).apply {
        set(default)
    }
