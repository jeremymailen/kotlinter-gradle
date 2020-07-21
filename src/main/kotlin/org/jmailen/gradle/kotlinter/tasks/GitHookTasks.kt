package org.jmailen.gradle.kotlinter.tasks

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class InstallPreCommitHookTask : InstallHookTask("pre-commit") {
    override val hookContent =
        """
            ${'$'}GRADLEW formatKotlin

            status=${'$'}?
            if [ "${'$'}status" != 0 ] ; then
                echo 1>&2 "\nformatKotlin had non-zero exit status, aborting commit"
                exit 1
            fi
        """.trimIndent()
}

open class InstallPrePushHookTask : InstallHookTask("pre-push") {
    override val hookContent =
        """
            ${'$'}GRADLEW lintKotlin

            status=${'$'}?
            if [ "${'$'}status" != 0 ] ; then
                echo 1>&2 "\nlintKotlin found problems, running formatKotlin; commit the result and re-push"
                ${'$'}GRADLEW formatKotlin
                exit 1
            fi
        """.trimIndent()
}

/**
 * Install or update a kotlinter-gradle hook.
 */
abstract class InstallHookTask(hookPath: String) : DefaultTask() {
    @get:Internal
    abstract val hookContent: String

    @get:Internal
    val dotGitDir = File(project.rootDir, ".git")

    @get:Internal
    val hookDir = File(dotGitDir.absolutePath, "hooks")

    @get:Internal
    val hookFile = File(hookDir, "/$hookPath")

    init {
        outputs.upToDateWhen { !shouldRun() }
    }

    private fun shouldRun(): Boolean {
        if (!hookDir.exists()) {
            return true
        }

        if (!hookFile.exists()) {
            return true
        }

        if (shouldUpdateHook()) {
            return true
        }

        return false
    }

    @TaskAction
    fun run() {
        if (!dotGitDir.exists() || !dotGitDir.isDirectory) {
            throw GradleException(".git directory not found at ${dotGitDir.path}")
        }
        logger.info(".git directory: $dotGitDir")

        if (!hookDir.exists()) {
            logger.debug("Creating hook dir $hookDir")
            hookDir.mkdir()
        }
        logger.info("hookDir: $hookDir")

        if (!hookFile.exists()) {
            logger.info("Creating $hookFile anew")
            hookFile.createNewFile()
            hookFile.setExecutable(true)
        }

        createOrUpdateHook()
        logger.quiet("Wrote hook to $hookFile")
    }

    private fun shouldUpdateHook(): Boolean {
        if (hookFile.length() == 0L) {
            return true
        }

        val hookFileContent = hookFile.readText()
        val startHookMarker = hookFileContent.indexOf(startHook)
        if (startHookMarker == -1) {
            return true
        }

        val endIndex = hookFileContent.indexOf(endHook)
        val newHookFileContent = hookFileContent.replaceRange(
            startHookMarker,
            endIndex,
            generateHook(gradleCommand, hookContent, includeEndHook = false)
        )
        if (newHookFileContent != hookFileContent) {
            return true
        }

        return false
    }

    /**
     * Create or update the hook file.
     *
     * @return True if the file was written to, false otherwise (e.g. hook already up-to-date)
     */
    private fun createOrUpdateHook() {
        return if (hookFile.length() == 0L) {
            logger.info("Writing hook to empty file")
            hookFile.writeText(generateHook(gradleCommand, hookContent, addShebang = true))
        } else {
            val hookFileContent = hookFile.readText()
            val startHookMarker = hookFileContent.indexOf(startHook)
            if (startHookMarker == -1) {
                logger.info("Appending hook to end of existing non-empty file")
                hookFile.appendText(generateHook(gradleCommand, hookContent))
            } else {
                val endIndex = hookFileContent.indexOf(endHook)
                val newHookFileContent = hookFileContent.replaceRange(
                    startHookMarker,
                    endIndex,
                    generateHook(gradleCommand, hookContent, includeEndHook = false)
                )
                if (newHookFileContent != hookFileContent) {
                    logger.info("Updating existing kotlinter-installed hook")
                    hookFile.writeText(newHookFileContent)
                } else {
                    logger.info("Not altering up-to-date hook")
                }
            }
        }
    }

    /**
     * Find or guess a Gradle command for use in the hook script.
     *
     * If the Gradle wrapper is found in the project root directory, that is used,
     * otherwise we hope for a `gradle` in the PATH
     */
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

        internal val shebang =
            """
            #!/bin/sh
            set -e
            """.trimIndent()

        /**
         * Generate the hook script
         */
        internal fun generateHook(
            gradlew: String,
            hookContent: String,
            addShebang: Boolean = false,
            includeEndHook: Boolean = true
        ): String = (if (addShebang) shebang else "") +
            """
            |$startHook
            |GRADLEW=$gradlew
            |$hookContent
            |${if (includeEndHook) endHook else ""}
            """.trimMargin()
    }
}
