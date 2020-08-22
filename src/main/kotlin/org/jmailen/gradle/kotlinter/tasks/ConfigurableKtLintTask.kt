package org.jmailen.gradle.kotlinter.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.SourceTask
import org.gradle.internal.exceptions.MultiCauseException
import org.jmailen.gradle.kotlinter.KotlinterExtension.Companion.DEFAULT_DISABLED_RULES
import org.jmailen.gradle.kotlinter.KotlinterExtension.Companion.DEFAULT_EXPERIMENTAL_RULES
import org.jmailen.gradle.kotlinter.support.KtLintParams

abstract class ConfigurableKtLintTask : SourceTask() {

    @get:Classpath
    val classpath = project.objects.fileCollection()

    @Input
    @Optional
    val indentSize = property<Int?>(default = null)

    @Input
    val experimentalRules = property(default = DEFAULT_EXPERIMENTAL_RULES)
    @Input
    val disabledRules = listProperty(default = DEFAULT_DISABLED_RULES.toList())

    @Internal
    protected fun getKtLintParams() = KtLintParams(
        indentSize = indentSize.orNull,
        experimentalRules = experimentalRules.get(),
        disabledRules = disabledRules.get()
    )
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

inline fun <reified T : Throwable> Throwable.workErrorCauses(): List<Throwable> {
    return when (this) {
        is MultiCauseException -> this.causes.map { it.cause }
        else -> listOf(this.cause)
    }.filter {
        it?.let {
            // class instance comparison doesn't work due to different classloaders
            it.javaClass.canonicalName == T::class.java.canonicalName
        } ?: false
    }.filterNotNull()
}
