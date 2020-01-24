package functional

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder

abstract class WithGradleTest {

    @get:Rule
    val testProjectDir = TemporaryFolder()

    protected fun build(vararg args: String): BuildResult = gradleRunnerFor(*args).build()

    protected fun buildAndFail(vararg args: String): BuildResult = gradleRunnerFor(*args).buildAndFail()

    private fun gradleRunnerFor(vararg args: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments(args.toList() + "--stacktrace")
            .withPluginClasspath()
            .forwardOutput()
}
