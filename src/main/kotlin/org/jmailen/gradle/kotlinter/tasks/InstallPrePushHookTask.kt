package org.jmailen.gradle.kotlinter.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Install or update the kotlinter-gradle pre-push hook.
 */
open class InstallPrePushHookTask : DefaultTask() {
    @TaskAction
    fun run() {
        val dotGitDir = File(project.rootDir, ".git")
        if (!dotGitDir.exists() || !dotGitDir.isDirectory) {
            throw GradleException(".git directory not found at ${dotGitDir.path}")
        }
        logger.info(".git directory: $dotGitDir")

        val hookDir = File(dotGitDir.absolutePath, "hooks").apply {
            if (!exists()) {
                logger.debug("Creating hook dir $this")
                mkdir()
            }
        }
        logger.quiet("hookDir: $hookDir")

        val prePushHookFile = File(hookDir, "/pre-push").apply {
            if (!exists()) {
                logger.info("Creating $this anew")
                createNewFile()
                setExecutable(true)
            }
        }

        if (prePushHookFile.length() == 0L) {
            logger.info("Writing hook to empty file")
            prePushHookFile.writeText(generateHook(gradleCommand, addShebang = true))
        } else {
            val prePushHookFileContent = prePushHookFile.readText()
            val startIndex = prePushHookFileContent.indexOf(startHook)
            if (startIndex == -1) {
                logger.info("Appending hook to end of existing non-empty file")
                prePushHookFile.appendText(generateHook(gradleCommand))
            } else {
                logger.info("Updating existing kotlinter-installed hook")
                val endIndex = prePushHookFileContent.indexOf(endHook)
                val newPrePushHookFileContent = prePushHookFileContent.replaceRange(
                    startIndex,
                    endIndex,
                    generateHook(gradleCommand, includeEndHook = false)
                )
                prePushHookFile.writeText(newPrePushHookFileContent)
            }
        }

        logger.quiet("Wrote hook to $prePushHookFile")
    }

    private val gradleCommand: String by lazy {
        val gradlewFilename = if (System.getProperty("os.name").toLowerCase().contains("win")) {
            "gradlew.bat"
        } else {
            "gradlew"
        }

        val gradlew = File(project.rootDir, gradlewFilename)
        if (gradlew.exists() && gradlew.isFile && gradlew.canExecute()) {
            logger.info("Using gradlew wrapper at ${gradlew.path}")
            gradlew.path
        } else {
            "gradle"
        }
    }

    companion object {
        internal const val startHook = "\n##### KOTLINTER HOOK START #####"

        internal const val endHook = "##### KOTLINTER HOOK END #####\n"

        internal val shebang = """
            #!/bin/sh
            set -e
        """.trimIndent()

        internal val hookContent = """
            ${'$'}GRADLEW lintKotlin

            status=${'$'}?
            if [ "${'$'}status" != 0 ] ; then
                echo 1>&2 "\nlintKotlin found problems; running formatKotlin command..."
                ${'$'}GRADLEW formatKotlin
                exit 1
            fi
        """.trimIndent()

        /**
         * Generate the hook script
         */
        internal fun generateHook(
            gradlew: String,
            addShebang: Boolean = false,
            includeEndHook: Boolean = true
        ): String = (if (addShebang) shebang else "") +
                """
                |$startHook
                |GRADLEW=$gradlew
                |$hookContent\n
                """.trimMargin() +
                (if (includeEndHook) endHook else "")
    }
}
