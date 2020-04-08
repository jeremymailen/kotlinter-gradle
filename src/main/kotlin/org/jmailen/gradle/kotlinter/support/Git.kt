package org.jmailen.gradle.kotlinter.support

import org.gradle.api.GradleException
import java.io.File

fun findGitDir(dir: File): File {
    return findInParents(".git", dir)
        ?: throw GradleException("Could not find .git directory; searched $dir and parents")
}

fun findGradlew(dir: File): File {
    return findInParents("gradlew", dir)
        ?: throw GradleException("Could not find .git directory; searched $dir and parents")
}

private tailrec fun findInParents(toFind: String, startFrom: File): File? {
    val gitDir = File(startFrom, toFind)
    if (gitDir.exists()) {
        return gitDir
    }

    if (startFrom.parentFile == null || !startFrom.parentFile.exists()) {
        return null
    }

    return findInParents(toFind, startFrom.parentFile)
}
