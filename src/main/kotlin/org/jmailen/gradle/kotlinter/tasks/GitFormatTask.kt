package org.jmailen.gradle.kotlinter.tasks

import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.workers.WorkerExecutor
import org.jmailen.gradle.kotlinter.tasks.format.FormatWorkerAction
import org.jmailen.gradle.kotlinter.tasks.format.GitFormatWorkerAction
import javax.inject.Inject

open class GitFormatTask @Inject constructor(
    workerExecutor: WorkerExecutor,
    objectFactory: ObjectFactory,
    projectLayout: ProjectLayout,
) : FormatTask(workerExecutor, objectFactory, projectLayout) {

    override fun executeFormat(
        filesToFormat: FileCollection,
        action: Class<out FormatWorkerAction>,
    ) {
        val changedFiles: Set<String>? = project.rootDir.asGitRepository { _, git ->
            val status = git.status().call()

            val modifiedTrackedFiles = (status.added + status.changed).ifEmpty {
                // if user not staged files then format all changes file
                status.modified
            }

            logger.warn("Founded modified tracked files $modifiedTrackedFiles")
            modifiedTrackedFiles
        }

        val changedFilesToFormat = source.filter {
            // jgit use is `/` is path separator because always
            val relativePath = it.toRelativeString(project.rootDir).replace('\\', '/')

            changedFiles?.contains(relativePath) ?: true
        }

        super.executeFormat(
            filesToFormat = changedFilesToFormat,
            action = GitFormatWorkerAction::class.java
        )
    }
}
