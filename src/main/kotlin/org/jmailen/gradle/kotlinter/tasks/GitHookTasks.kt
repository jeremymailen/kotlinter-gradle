package org.jmailen.gradle.kotlinter.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.jmailen.gradle.kotlinter.support.VersionProperties
import java.io.File

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
abstract class InstallHookTask(hookFileName: String) : DefaultTask() {
    @Internal val hooksDir = project.rootProject.file(".git/hooks").apply { mkdirs() }
    @Internal val hookFile = File(hooksDir, hookFileName).apply {
        createNewFile().and(setExecutable(true))
    }
    @get:Internal
    val hookFileContent by lazy { hookFile.readText() }

    @get:Internal
    abstract val hookContent: String

    init {
        outputs.upToDateWhen {
            hookFileContent.contains(hookVersion)
        }
    }

    @TaskAction
    fun run() {

        if (hookFileContent.isEmpty()) {
            logger.info("creating hook file: $hookFile")
            hookFile.writeText(generateHook(gradleCommand, hookContent, addShebang = true))
        } else {
            val startIndex = hookFileContent.indexOf(startHook)
            if (startIndex == -1) {
                logger.info("adding hook to file: $hookFile")
                hookFile.appendText(generateHook(gradleCommand, hookContent))
            } else {
                logger.info("replacing hook in file: $hookFile")
                val endIndex = hookFileContent.indexOf(endHook)
                val newHookFileContent = hookFileContent.replaceRange(
                    startIndex,
                    endIndex,
                    generateHook(gradleCommand, hookContent, includeEndHook = false)
                )
                hookFile.writeText(newHookFileContent)
            }
        }

        logger.quiet("Wrote hook to $hookFile")
    }

    private val gradleCommand: String by lazy {
        val gradlewFilename = if (System.getProperty("os.name").contains("win", true)) {
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
        private val version = VersionProperties().version()

        internal val startHook = "\n##### KOTLINTER HOOK START #####"

        internal val hookVersion = "##### KOTLINTER $version #####"

        internal val endHook = "##### KOTLINTER HOOK END #####\n"

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
                |$hookVersion
                |GRADLEW=$gradlew
                |$hookContent
                |${if (includeEndHook) endHook else ""}
                """.trimMargin()
    }
}
