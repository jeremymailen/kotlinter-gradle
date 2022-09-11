package org.jmailen.gradle.kotlinter.tasks

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.IgnoreEmptyDirectories
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

    @Input
    val workerMaxHeapSize: Property<String> = objectFactory.property(default = "256m")

    @Classpath
    val ktlintClasspath: ConfigurableFileCollection = objectFactory.fileCollection()

    @Classpath
    val ruleSetsClasspath: ConfigurableFileCollection = objectFactory.fileCollection()

    @Internal
    val sourceFiles = project.objects.fileCollection()

    @SkipWhenEmpty // Marks the input incremental: https://github.com/gradle/gradle/issues/17593
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    @IgnoreEmptyDirectories
    val source = objectFactory.fileCollection().from({ sourceFiles.asFileTree.matching(patternSet) })

    override fun source(vararg sources: Any?): SourceTask {
        sourceFiles.setFrom(*sources)
        return this
    }

    override fun getSource(): FileTree = source.asFileTree

    override fun setSource(source: Any) {
        sourceFiles.from(source)
    }

    @Internal
    protected fun getKtLintParams(): KtLintParams = KtLintParams(
        experimentalRules = experimentalRules.get(),
        disabledRules = disabledRules.get(),
    )
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

internal fun ConfigurableKtLintTask.getChangedEditorconfigFiles(inputChanges: InputChanges) =
    if (inputChanges.isIncremental) {
        inputChanges.getFileChanges(editorconfigFiles).map(FileChange::getFile)
    } else {
        emptyList()
    }

internal fun ConfigurableKtLintTask.getChangedSources(inputChanges: InputChanges) =
    if (inputChanges.isIncremental && inputChanges.getFileChanges(editorconfigFiles).none()) {
        inputChanges.getFileChanges(source).map(FileChange::getFile)
    } else {
        source
    }
