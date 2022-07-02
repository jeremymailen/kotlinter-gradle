package org.jmailen.gradle.kotlinter.tasks

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.GradleException
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jmailen.gradle.kotlinter.support.KotlinterError
import org.jmailen.gradle.kotlinter.tasks.format.FormatWorkerAction
import javax.inject.Inject

open class FormatTask @Inject constructor(
    private val workerExecutor: WorkerExecutor,
    objectFactory: ObjectFactory,
    private val projectLayout: ProjectLayout,
) : ConfigurableKtLintTask(
    projectLayout = projectLayout,
    objectFactory = objectFactory,
) {

    @OutputFile
    @Optional
    val report: RegularFileProperty = objectFactory.fileProperty()

    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun run() {
        val changedFiles: Set<String>? = FileRepositoryBuilder()
            .setWorkTree(project.rootDir)
            .setMustExist(false)
            .readEnvironment() // scan environment GIT_* variables
            .findGitDir() // scan up the file system tree
            .build().use { repository ->
                if (!repository.objectDatabase.exists()) {
                    logger.warn("Not found git repository")
                    return@use null
                }

                Git(repository).use { git ->
                    val status = git.status().call()

                    val modifiedTrackedFiles = (status.added + status.changed).ifEmpty {
                        // if user not staged files then format all changes file
                        status.modified
                    }

                    logger.warn("Founded modified tracked files $modifiedTrackedFiles")
                    modifiedTrackedFiles
                }
            }

        val result = with(workerExecutor.noIsolation()) {
            submit(FormatWorkerAction::class.java) { p ->
                p.name.set(name)
                p.files.from(
                    source.filter {
                        // jgit use is `/` is path separator because always
                        val relativePath = it.toRelativeString(project.rootDir).replace('\\', '/')

                        changedFiles?.contains(relativePath) ?: true
                    }
                )
                p.projectDirectory.set(projectLayout.projectDirectory.asFile)
                p.gitProjectDirectory.set(project.rootDir)
                p.ktLintParams.set(getKtLintParams())
                p.output.set(report)
            }
            runCatching { await() }
        }

        result.exceptionOrNull()?.workErrorCauses<KotlinterError>()?.ifNotEmpty {
            forEach { logger.error(it.message, it.cause) }
            throw GradleException("error formatting sources for $name")
        }
    }
}
