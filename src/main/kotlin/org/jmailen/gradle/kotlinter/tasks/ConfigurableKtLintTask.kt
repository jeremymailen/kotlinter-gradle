package org.jmailen.gradle.kotlinter.tasks

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceTask
import org.gradle.work.FileChange
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.workers.WorkerExecutionException
import org.jmailen.gradle.kotlinter.KotlinterExtension.Companion.DEFAULT_IGNORE_LINT_FAILURES
import org.jmailen.gradle.kotlinter.support.findApplicableEditorConfigFiles

abstract class ConfigurableKtLintTask(projectLayout: ProjectLayout, objectFactory: ObjectFactory) : SourceTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Incremental
    internal val editorconfigFiles: FileCollection = objectFactory.fileCollection().apply {
        from(projectLayout.findApplicableEditorConfigFiles().toList())
    }

    @Input
    open val ignoreLintFailures: Property<Boolean> = objectFactory.property(default = DEFAULT_IGNORE_LINT_FAILURES)

    @Classpath
    val ktlintClasspath: ConfigurableFileCollection = objectFactory.fileCollection()

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

fun WorkerExecutionException.hasRootCause(type: Class<*>): Boolean {
    // this is lame, but serialized across worker boundaries exceptions are not comparable
    // and recursive cause checking runs into serialized placeholder exceptions
    return this.stackTraceToString().contains(type.canonicalName)
}
