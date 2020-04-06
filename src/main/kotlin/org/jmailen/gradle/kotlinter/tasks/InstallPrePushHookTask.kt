package org.jmailen.gradle.kotlinter.tasks

import com.google.common.annotations.VisibleForTesting
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import java.io.File

open class InstallPrePushHookTask : DefaultTask() {
    @TaskAction
    fun run() {
        val dotGitDir = foo(project.rootDir)

        val hookDir = File(dotGitDir.absolutePath, "hooks")
        if (!hookDir.exists()) {
            logger.debug("Creating hook dir $hookDir")
            hookDir.mkdirs()
        }
        logger.info("hookDir: $hookDir")

        val prePushHookOutputFile = File(hookDir, "/pre-push")

        // TODO if file exists, don't overwrite it
        prePushHookOutputFile.writeText(prePushHook)
        prePushHookOutputFile.setExecutable(true)
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

        @VisibleForTesting
        fun foo(dir: File): File =
            findGitDir(dir) ?: throw GradleException("Could not find .git directory; searched $dir and parents")

        private tailrec fun findGitDir(dir: File): File? {
            val gitDir = File(dir, ".git")
            if (gitDir.exists()) {
                return gitDir
            }

            if (dir.parentFile == null || !dir.parentFile.exists()) {
                return null
            }

            return findGitDir(dir.parentFile)
        }
    }
}
