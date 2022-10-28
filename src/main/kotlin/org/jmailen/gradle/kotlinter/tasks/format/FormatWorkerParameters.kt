package org.jmailen.gradle.kotlinter.tasks.format

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkParameters
import org.jmailen.gradle.kotlinter.support.KtLintParams
import java.io.File

interface FormatWorkerParameters : WorkParameters {
    val name: Property<String>
    val changedEditorConfigFiles: ConfigurableFileCollection
    val files: ConfigurableFileCollection
    val projectDirectory: RegularFileProperty
    val ktLintParams: Property<KtLintParams>
    val output: RegularFileProperty
    val baselineFile: RegularFileProperty
}
