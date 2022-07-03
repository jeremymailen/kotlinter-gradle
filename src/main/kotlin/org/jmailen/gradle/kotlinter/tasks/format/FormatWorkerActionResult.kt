package org.jmailen.gradle.kotlinter.tasks.format

import java.io.File

internal class FormatWorkerActionResult(
    val msg: List<String>,
    val formattedFiles: List<File>,
)
