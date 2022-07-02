package org.jmailen.gradle.kotlinter.tasks.format

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkParameters
import org.jmailen.gradle.kotlinter.support.KtLintParams

interface FormatWorkerParameters : WorkParameters {
    val name: Property<String>
    val files: ConfigurableFileCollection
    val projectDirectory: RegularFileProperty
    val gitProjectDirectory: RegularFileProperty
    val ktLintParams: Property<KtLintParams>
    val output: RegularFileProperty
}
