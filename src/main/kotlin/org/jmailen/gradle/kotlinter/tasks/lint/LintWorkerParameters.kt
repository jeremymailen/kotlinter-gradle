package org.jmailen.gradle.kotlinter.tasks.lint

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkParameters
import org.jmailen.gradle.kotlinter.support.KtLintParams
import org.jmailen.gradle.kotlinter.support.ReporterType
import java.io.File

internal interface LintWorkerParameters : WorkParameters {
    val name: Property<String>
    val changedEditorconfigFiles: ConfigurableFileCollection
    val files: ConfigurableFileCollection
    val projectDirectory: RegularFileProperty
    val reporters: MapProperty<ReporterType, File>
    val ktLintParams: Property<KtLintParams>
}
