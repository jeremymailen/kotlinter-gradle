package org.jmailen.gradle.kotlinter.support

fun userData(indentSize: Int, continuationIndentSize: Int, filePath: String) = mapOf(
    "indent_size" to indentSize.toString(),
    "continuation_indent_size" to continuationIndentSize.toString(),
    "file_path" to filePath
)
