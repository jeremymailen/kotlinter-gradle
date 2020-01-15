package org.jmailen.gradle.kotlinter.functional

import java.io.File
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder

internal abstract class WithGradleTest {

    @get:Rule
    val testProjectDir = TemporaryFolder()

    protected fun build(vararg args: String): BuildResult = gradleRunnerFor(*args).build()

    protected fun buildAndFail(vararg args: String): BuildResult = gradleRunnerFor(*args).buildAndFail()

    private fun gradleRunnerFor(vararg args: String): GradleRunner {
        return GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments(args.toList() + "--stacktrace")
            .forwardOutput()
            .withPluginClasspath()
    }

    protected fun File.resolve(path: String, receiver: File.() -> Unit): File =
        resolve(path).apply {
            parentFile.mkdirs()
            receiver()
        }
}
