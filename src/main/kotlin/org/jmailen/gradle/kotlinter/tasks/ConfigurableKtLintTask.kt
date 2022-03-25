package org.jmailen.gradle.kotlinter.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SourceTask
import org.gradle.internal.exceptions.MultiCauseException
import org.jmailen.gradle.kotlinter.KotlinterExtension.Companion.DEFAULT_DISABLED_RULES
import org.jmailen.gradle.kotlinter.KotlinterExtension.Companion.DEFAULT_EXPERIMENTAL_RULES
import org.jmailen.gradle.kotlinter.support.KtLintParams

abstract class ConfigurableKtLintTask : SourceTask() {

    @Input
    val experimentalRules: Property<Boolean> = property(default = DEFAULT_EXPERIMENTAL_RULES)

    @Input
    val disabledRules: ListProperty<String> = listProperty(default = DEFAULT_DISABLED_RULES.toList())

    @Internal
    protected fun getKtLintParams(): KtLintParams = KtLintParams(
        experimentalRules = experimentalRules.get(),
        disabledRules = disabledRules.get()
    )
}

internal inline fun <reified T> DefaultTask.property(default: T? = null): Property<T> =
    project.objects.property(T::class.java).apply {
        set(default)
    }

internal inline fun <reified T> DefaultTask.listProperty(default: Iterable<T> = emptyList()): ListProperty<T> =
    project.objects.listProperty(T::class.java).apply {
        set(default)
    }

internal inline fun <reified K, reified V> DefaultTask.mapProperty(default: Map<K, V> = emptyMap()): MapProperty<K, V> =
    project.objects.mapProperty(K::class.java, V::class.java).apply {
        set(default)
    }

inline fun <reified T : Throwable> Throwable.workErrorCauses(): List<Throwable> {
    return when (this) {
        is MultiCauseException -> this.causes.map { it.cause }
        else -> listOf(this.cause)
    }.filter {
        // class instance comparison doesn't work due to different classloaders
        it?.javaClass?.canonicalName == T::class.java.canonicalName
    }.filterNotNull()
}
