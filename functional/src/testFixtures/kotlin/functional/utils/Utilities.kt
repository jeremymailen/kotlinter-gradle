package functional.utils

import java.io.File

fun File.resolve(path: String, receiver: File.() -> Unit): File =
    resolve(path).apply {
        parentFile.mkdirs()
        receiver()
    }
