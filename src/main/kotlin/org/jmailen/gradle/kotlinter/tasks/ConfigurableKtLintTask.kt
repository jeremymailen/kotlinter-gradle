package org.jmailen.gradle.kotlinter.tasks

import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceTask
import org.gradle.internal.exceptions.MultiCauseException
import org.gradle.work.FileChange
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.jmailen.gradle.kotlinter.KotlinterExtension.Companion.DEFAULT_IGNORE_FAILURES
import org.jmailen.gradle.kotlinter.support.findApplicableEditorConfigFiles

abstract class ConfigurableKtLintTask(
    projectLayout: ProjectLayout,
    objectFactory: ObjectFactory,
) : SourceTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Incremental
    internal val editorconfigFiles: FileCollection = objectFactory.fileCollection().apply {
        from(projectLayout.findApplicableEditorConfigFiles().toList())
    }

    @Input
    open val ignoreFailures: Property<Boolean> = objectFactory.property(default = DEFAULT_IGNORE_FAILURES)

    protected fun getChangedEditorconfigFiles(inputChanges: InputChanges) =
        inputChanges.getFileChanges(editorconfigFiles).map(FileChange::getFile)
}

internal inline fun <reified T> ObjectFactory.property(default: T? = null): Property<T> = property(T::class.java).apply {
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

inline fun <reified T : Throwable> Throwable.workErrorCauses(): List<Throwable> {
    return when (this) {
        is MultiCauseException -> this.causes.map { it.cause }
        else -> listOf(this.cause)
    }.filter {
        // class instance comparison doesn't work due to different classloaders
        it?.javaClass?.canonicalName == T::class.java.canonicalName
    }.filterNotNull()
}
