package org.jmailen.gradle.kotlinter.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.jmailen.gradle.kotlinter.support.VersionProperties
import java.io.File

abstract class InstallPreCommitHookTask : InstallHookTask("pre-commit") {
    override val hookContent =
        """
            if ! ${'$'}GRADLEW formatKotlin ; then
                echo 1>&2 "\nformatKotlin had non-zero exit status, aborting commit"
                exit 1
            fi
        """.trimIndent()
}

abstract class InstallPrePushHookTask : InstallHookTask("pre-push") {
    override val hookContent =
        """
            if ! ${'$'}GRADLEW lintKotlin ; then
                echo 1>&2 "\nlintKotlin found problems, running formatKotlin; commit the result and re-push"
                ${'$'}GRADLEW formatKotlin
                exit 1
            fi
        """.trimIndent()
}

/**
 * Install or update a kotlinter-gradle hook.
 */
abstract class InstallHookTask(@get:Internal val hookFileName: String) : DefaultTask() {

    @Input
    val gitDirPath: Property<String> = project.objects.property(default = ".git")

    @Input
    val rootProjectDir = project.objects.property(default = project.rootProject.rootDir)

    @get:Internal
    abstract val hookContent: String

    init {
        outputs.upToDateWhen {
            getHookFile()?.readText()?.contains(hookVersion) ?: false
        }
    }

    @TaskAction
    fun run() {
        val hookFile = getHookFile(true) ?: return
        val hookFileContent = hookFile.readText()

        if (hookFileContent.isEmpty()) {
            logger.info("creating hook file: $hookFile")
            hookFile.writeText(generateHook(gradleCommand, hookContent, addShebang = true))
        } else {
            val startIndex = hookFileContent.indexOf(START_HOOK)
            if (startIndex == -1) {
                logger.info("adding hook to file: $hookFile")
                hookFile.appendText(generateHook(gradleCommand, hookContent))
            } else {
                logger.info("replacing hook in file: $hookFile")
                val endIndex = hookFileContent.indexOf(END_HOOK)
                val newHookFileContent = hookFileContent.replaceRange(
                    startIndex,
                    endIndex,
                    generateHook(gradleCommand, hookContent, includeEndHook = false),
                )
                hookFile.writeText(newHookFileContent)
            }
        }

        logger.quiet("Wrote hook to $hookFile")
    }

    private fun getHookFile(warn: Boolean = false): File? {
        val gitDir = File(rootProjectDir.get(), gitDirPath.get())
        if (!gitDir.isDirectory) {
            if (warn) logger.warn("skipping hook creation because $gitDir is not a directory")
            return null
        }
        return try {
            val hooksDir = File(gitDir, "hooks").apply { mkdirs() }
            File(hooksDir, hookFileName).apply {
                createNewFile().and(setExecutable(true))
            }
        } catch (e: Exception) {
            if (warn) logger.warn("skipping hook creation because could not create hook under $gitDir: ${e.message}")
            null
        }
    }

    private val gradleCommand: String by lazy {
        val gradlewFilename = if (System.getProperty("os.name").contains("win", true)) {
            "gradlew.bat"
        } else {
            "gradlew"
        }

        val gradlew = File(rootProjectDir.get(), gradlewFilename)
        if (gradlew.exists() && gradlew.isFile && gradlew.canExecute()) {
            logger.info("Using gradlew wrapper at ${gradlew.invariantSeparatorsPath}")
            gradlew.invariantSeparatorsPath
        } else {
            "gradle"
        }
    }

    companion object {
        private val version = VersionProperties().version()

        internal const val START_HOOK = "\n##### KOTLINTER HOOK START #####"

        internal val hookVersion = "##### KOTLINTER $version #####"

        internal const val END_HOOK = "##### KOTLINTER HOOK END #####\n"

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
            includeEndHook: Boolean = true,
        ): String = (if (addShebang) shebang else "") +
            """
                |$START_HOOK
                |$hookVersion
                |GRADLEW=$gradlew
                |$hookContent
                |${if (includeEndHook) END_HOOK else ""}
            """.trimMargin()
    }
}
