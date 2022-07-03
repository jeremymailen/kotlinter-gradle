package org.jmailen.gradle.kotlinter.tasks.format

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.internal.logging.slf4j.DefaultContextAwareTaskLogger
import org.jmailen.gradle.kotlinter.tasks.GitFormatTask
import org.jmailen.gradle.kotlinter.tasks.asGitRepository
import java.io.File

abstract class GitFormatWorkerAction : FormatWorkerAction() {

    private val logger: Logger = DefaultContextAwareTaskLogger(Logging.getLogger(GitFormatTask::class.java))


    private val gitProjectDirectory: File = parameters.gitProjectDirectory.asFile.get()

    override fun formatAngGetResult(): FormatWorkerActionResult {
        logger.error("SDGFSDGSDG")

        return super.formatAngGetResult().also { result ->
            if (result.formattedFiles.isEmpty()) {
                return@also
            }
/*            gitProjectDirectory.asGitRepository { _, git ->
                result.formattedFiles
                    .forEach { fileResult ->
                        val relativePath = fileResult.toRelativeString(gitProjectDirectory).replace('\\', '/')

                        // TODO not working because index becomes broken
                        //git.add().setUpdate(true).addFilepattern(relativePath).call()
                    }
            } ?: logger.error("Not found repository")*/
        }
    }
}
