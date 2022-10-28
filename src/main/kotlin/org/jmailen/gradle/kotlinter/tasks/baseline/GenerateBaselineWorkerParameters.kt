package org.jmailen.gradle.kotlinter.tasks.baseline

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkParameters
import org.jmailen.gradle.kotlinter.support.KtLintParams

internal interface GenerateBaselineWorkerParameters : WorkParameters {
    val name: Property<String>
    val changedEditorconfigFiles: ConfigurableFileCollection
    val files: ConfigurableFileCollection
    val projectDirectory: RegularFileProperty
    val ktLintParams: Property<KtLintParams>
    val baselineFile: RegularFileProperty
}
