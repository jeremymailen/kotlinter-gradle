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
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceTask
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.FileChange
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutionException
import org.gradle.workers.WorkerExecutor
import org.jmailen.gradle.kotlinter.KotlinterExtension.Companion.DEFAULT_IGNORE_LINT_FAILURES
import org.jmailen.gradle.kotlinter.support.defaultWorkerJvmArgs
import org.jmailen.gradle.kotlinter.support.findApplicableEditorConfigFiles

@DisableCachingByDefault(because = "Base class for lint and format task implementations")
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

    /**
     * Arguments passed to the JVM of the worker process running ktlint.
     *
     * Defaults to `--sun-misc-unsafe-memory-access=allow` on JDK 24+, which suppresses the `sun.misc.Unsafe` deprecation
     * warning printed by the Kotlin compiler embedded in ktlint. Use `add` to keep that default, or `set` to replace it.
     *
     * Not a task input: these arguments don't affect lint results, so they must not invalidate build cache entries
     * between machines running different JDKs.
     */
    @get:Internal
    val workerJvmArgs: ListProperty<String> = objectFactory.listProperty(default = defaultWorkerJvmArgs())

    protected fun getChangedEditorconfigFiles(inputChanges: InputChanges) =
        inputChanges.getFileChanges(editorconfigFiles).map(FileChange::getFile)

    protected fun WorkerExecutor.ktlintWorkQueue(): WorkQueue = processIsolation { config ->
        config.classpath.setFrom(ktlintClasspath)
        config.forkOptions { forkOptions -> forkOptions.jvmArgs(workerJvmArgs.get()) }
    }
}

internal inline fun <reified T : Any> ObjectFactory.property(default: T? = null): Property<T> = property(T::class.java).apply {
    set(default)
}

internal inline fun <reified T : Any> ObjectFactory.listProperty(default: Iterable<T> = emptyList()): ListProperty<T> =
    listProperty(T::class.java).apply {
        set(default)
    }

internal inline fun <reified K : Any, reified V : Any> ObjectFactory.mapProperty(default: Map<K, V> = emptyMap()): MapProperty<K, V> =
    mapProperty(K::class.java, V::class.java).apply {
        set(default)
    }

fun WorkerExecutionException.hasRootCause(type: Class<*>): Boolean {
    // this is lame, but serialized across worker boundaries exceptions are not comparable
    // and recursive cause checking runs into serialized placeholder exceptions
    return this.stackTraceToString().contains(type.canonicalName)
}
