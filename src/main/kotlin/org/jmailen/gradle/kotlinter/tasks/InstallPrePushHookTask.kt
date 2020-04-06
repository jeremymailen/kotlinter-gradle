package org.jmailen.gradle.kotlinter.tasks

import com.google.common.annotations.VisibleForTesting
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

open class InstallPrePushHookTask : DefaultTask() {
    private val dotGitDir = findGitDir(project.rootDir)

    @OutputFile
    val prePushHookOutputFile = File(dotGitDir, "/hooks/pre-push")

    @TaskAction
    fun run() {
        if (dotGitDir == null) {
            logger.warn("Could not find .git directory")
            return
        }
        logger.info("dotGitDir: $dotGitDir")

        val hookDir = File(dotGitDir.absolutePath, "hooks")
        if (!hookDir.exists()) {
            logger.debug("Creating hook dir $hookDir")
            hookDir.mkdirs()
        }
        logger.info("hookDir: $hookDir")

        // TODO if file exists, don't overwrite it
        prePushHookOutputFile.writeText(Companion.prePushHook)
        prePushHookOutputFile.setExecutable(true)
    }

    @VisibleForTesting
    protected fun findGitDir(dir: File): File? {
        val gitDir = File(dir, ".git")
        if (gitDir.exists()) {
            return gitDir
        }
        if (!dir.parentFile.exists()) {
            // at top of hierarchy
            return null
        }
        return findGitDir(dir.parentFile)
    }

    companion object {
        private val prePushHook = """
            #!/bin/sh
            GRADLE=./gradlew
            LINT_COMMAND="${'$'}GRADLE lintKotlin"
            FORMAT_COMMAND="${'$'}GRADLE formatKotlin"

            ${'$'}LINT_COMMAND

            status=${'$'}?
            if [ "${'$'}status" != 0 ] ; then
                echo 1>&2 "\n${'$'}LINT_COMMAND found problems; running ${'$'}FORMAT_COMMAND..."
                ${'$'}FORMAT_COMMAND
                exit 1
            fi

            exit 0
        """.trimIndent()
    }
}
