package org.jmailen.gradle.kotlinter.support

import com.google.common.annotations.VisibleForTesting
import org.gradle.api.GradleException
import java.io.File

/**
 * Find the nearest .git directory
 */
fun findGitDir(dir: File): File {
    return findInParents(".git", dir)
        ?: throw GradleException("Could not find .git directory; searched $dir and parents")
}

/**
 * Find the Gradle wrapper
 */
fun findGradlew(dir: File): File {
    return findInParents("gradlew", dir)
        ?: throw GradleException("Could not find gradlew; searched $dir and parents")
}

/**
 * Search this directory and parent directories for a file by name
 *
 * @return The file or null if a file of hte given name was not found in any parent directories
 */
@VisibleForTesting
internal tailrec fun findInParents(name: String, startFrom: File): File? {
    val file = File(startFrom, name)
    if (file.exists()) {
        return file
    }

    if (startFrom.parentFile == null || !startFrom.parentFile.exists()) {
        return null
    }

    return findInParents(name, startFrom.parentFile)
}
