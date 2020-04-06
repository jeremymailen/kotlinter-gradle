package org.jmailen.gradle.kotlinter.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.jmailen.gradle.kotlinter.support.findGitDir
import java.io.File

open class InstallPrePushHookTask : DefaultTask() {
    @TaskAction
    fun run() {
        val dotGitDir = findGitDir(project.rootDir)
        logger.info(".git directory: $dotGitDir")

        val hookDir = File(dotGitDir.absolutePath, "hooks")
        if (!hookDir.exists()) {
            logger.debug("Creating hook dir $hookDir")
            hookDir.mkdir()
        }
        logger.info("hookDir: $hookDir")

        val prePushHookFile = File(hookDir, "/pre-push").apply {
            if (!exists()) {
                logger.info("Creating $this anew")
                createNewFile()
                setExecutable(true)
            }
        }

        if (prePushHookFile.length() == 0L) {
            logger.info("Writing hook to empty file")
            prePushHookFile.writeText(generateHook(true))
            return
        }

        val prePushHookFileContent = prePushHookFile.readText()
        val startIndex = prePushHookFileContent.indexOf(startHook)
        if (startIndex == -1) {
            logger.info("Replacing existing hook")
            val endIndex = prePushHookFileContent.indexOf(endHook)
            prePushHookFileContent.replaceRange(startIndex, endIndex, generateHook())
        } else {
            logger.info("Appending hook to end of existing non-empty file")
            prePushHookFile.appendText(generateHook())
        }
    }

    companion object {
        private const val startHook = "##### KOTLINTER HOOK START #####"
        private const val endHook = "##### KOTLINTER HOOK END #####"
        private const val shebang = """
            #!/bin/sh
            set -e
        """
        // TODO workdir should be project root
        private const val prePushHook = """
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
        """

        private fun generateHook(addShebang: Boolean = false): String {
            return """  ${if (addShebang) shebang else ""}
                    $startHook
                    $prePushHook
                    $endHook
                """.trimIndent()
        }
    }
}
