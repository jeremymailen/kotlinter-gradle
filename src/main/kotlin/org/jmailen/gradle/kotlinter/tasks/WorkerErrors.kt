package org.jmailen.gradle.kotlinter.tasks

import java.io.File

internal fun workerErrorMessage(action: String, file: File?, error: Throwable): String {
    val fileContext = file?.let { " while processing ${it.path}" }.orEmpty()
    val errorContext = error.message?.let { ": $it" }.orEmpty()
    return "$action worker execution error$fileContext$errorContext"
}
