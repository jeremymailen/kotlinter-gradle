package org.jmailen.gradle.kotlinter.tasks

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File

fun <T> File.asGitRepository(
    invoker: (repository: Repository, git: Git) -> T
): T? {
    return FileRepositoryBuilder()
        .setWorkTree(this)
        .setMustExist(false)
        .readEnvironment()
        .findGitDir()
        .build().use { repository ->
            if (!repository.objectDatabase.exists()) {
                return@use null
            }

            Git(repository).use { git ->
                invoker.invoke(repository, git)
            }
        }
}
