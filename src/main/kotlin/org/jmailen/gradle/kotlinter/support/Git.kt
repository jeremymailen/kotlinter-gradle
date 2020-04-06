package org.jmailen.gradle.kotlinter.support

import org.gradle.api.GradleException
import java.io.File


fun findGitDir(dir: File): File {
    return findGitDirNullable(dir) ?: throw GradleException("Could not find .git directory; searched $dir and parents")
}

private tailrec fun findGitDirNullable(dir: File): File? {
    val gitDir = File(dir, ".git")
    if (gitDir.exists()) {
        return gitDir
    }

    if (dir.parentFile == null || !dir.parentFile.exists()) {
        return null
    }

    return findGitDirNullable(dir.parentFile)
}
