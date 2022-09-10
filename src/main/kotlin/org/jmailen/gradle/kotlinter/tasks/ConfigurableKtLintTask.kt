package org.jmailen.gradle.kotlinter.tasks

import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceTask
import org.gradle.work.FileChange
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.jmailen.gradle.kotlinter.KotlinterExtension.Companion.DEFAULT_DISABLED_RULES
import org.jmailen.gradle.kotlinter.KotlinterExtension.Companion.DEFAULT_EXPERIMENTAL_RULES
import org.jmailen.gradle.kotlinter.support.KtLintParams
import org.jmailen.gradle.kotlinter.support.findApplicableEditorConfigFiles

abstract class ConfigurableKtLintTask(
    projectLayout: ProjectLayout,
    objectFactory: ObjectFactory,
) : SourceTask() {

    @Input
    val experimentalRules: Property<Boolean> = objectFactory.property(default = DEFAULT_EXPERIMENTAL_RULES)

    @Input
    val disabledRules: ListProperty<String> = objectFactory.listProperty(default = DEFAULT_DISABLED_RULES.toList())

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Incremental
    internal val editorconfigFiles: FileCollection = objectFactory.fileCollection().apply {
        from(projectLayout.findApplicableEditorConfigFiles().toList())
    }

    @Internal
    val workerMaxHeapSize = objectFactory.property(default = "256m")

    @Internal
    protected fun getKtLintParams(): KtLintParams = KtLintParams(
        experimentalRules = experimentalRules.get(),
        disabledRules = disabledRules.get(),
    )

    protected fun getChangedEditorconfigFiles(inputChanges: InputChanges) =
        if (inputChanges.isIncremental) {
            inputChanges.getFileChanges(editorconfigFiles).map(FileChange::getFile)
        } else {
            emptyList()
        }
}

internal inline fun <reified T> ObjectFactory.property(default: T? = null): Property<T> =
    property(T::class.java).apply {
        set(default)
    }

internal inline fun <reified T> ObjectFactory.listProperty(default: Iterable<T> = emptyList()): ListProperty<T> =
    listProperty(T::class.java).apply {
        set(default)
    }

internal inline fun <reified K, reified V> ObjectFactory.mapProperty(default: Map<K, V> = emptyMap()): MapProperty<K, V> =
    mapProperty(K::class.java, V::class.java).apply {
        set(default)
    }
