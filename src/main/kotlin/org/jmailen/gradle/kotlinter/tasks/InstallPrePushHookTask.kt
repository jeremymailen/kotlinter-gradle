package org.jmailen.gradle.kotlinter.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.jmailen.gradle.kotlinter.support.findGitDir
import org.jmailen.gradle.kotlinter.support.findGradlew
import java.io.File

open class InstallPrePushHookTask : DefaultTask() {
    @TaskAction
    fun run() {
        val dotGitDir = findGitDir(project.rootDir)
        logger.info(".git directory: $dotGitDir")
        val gradlew = findGradlew(project.rootDir).path

        val hookDir = File(dotGitDir.absolutePath, "hooks").apply {
            if (!exists()) {
                logger.debug("Creating hook dir $this")
                mkdir()
            }
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
            prePushHookFile.writeText(generateHook(gradlew, addShebang = true))
            return
        }

        val prePushHookFileContent = prePushHookFile.readText()
        val startIndex = prePushHookFileContent.indexOf(startHook)
        if (startIndex == -1) {
            logger.info("Appending hook to end of existing non-empty file")
            prePushHookFile.appendText(generateHook(gradlew))
        } else {
            logger.info("Replacing existing hook")
            val endIndex = prePushHookFileContent.indexOf(endHook)
            prePushHookFileContent.replaceRange(startIndex, endIndex, generateHook(gradlew, includeEndHook = false))
        }
    }

    companion object {
        private const val startHook = "##### KOTLINTER HOOK START #####"

        private const val endHook = "##### KOTLINTER HOOK END #####\n"

        private const val shebang = """
            #!/bin/sh
            set -e
        """

        private const val prePushHook = """
            ${'$'}GRADLEW lintKotlin

            status=${'$'}?
            if [ "${'$'}status" != 0 ] ; then
                echo 1>&2 "\nlintKotlin found problems; running formatKotlin command..."
                ${'$'}GRADLEW formatKotlin
                exit 1
            fi
        """

        private fun generateHook(
            gradlew: String,
            addShebang: Boolean = false,
            includeEndHook: Boolean = true
        ): String {
            return """
                    ${if (addShebang) shebang else ""}
                    $startHook
                    GRADLEW=$gradlew

                    $prePushHook
                    ${if (includeEndHook) endHook else ""}
                """.trimIndent()
        }
    }
}
