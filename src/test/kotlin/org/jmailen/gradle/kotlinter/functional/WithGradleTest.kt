package org.jmailen.gradle.kotlinter.functional

import org.gradle.internal.classpath.DefaultClassPath
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.internal.PluginUnderTestMetadataReading
import org.junit.Rule
import org.junit.rules.TemporaryFolder

abstract class WithGradleTest {

    @get:Rule
    val testProjectDir = TemporaryFolder()

    protected fun build(vararg args: String): BuildResult = gradleRunnerFor(*args).build()

    protected fun buildAndFail(vararg args: String): BuildResult = gradleRunnerFor(*args).buildAndFail()

    protected abstract fun gradleRunnerFor(vararg args: String): GradleRunner

    abstract class Android : WithGradleTest() {

        override fun gradleRunnerFor(vararg args: String): GradleRunner {
            return defaultRunner(*args)
                .withPluginClasspath()
        }
    }

    abstract class Kotlin : WithGradleTest() {

        override fun gradleRunnerFor(vararg args: String): GradleRunner {
            val classpath = DefaultClassPath.of(PluginUnderTestMetadataReading.readImplementationClasspath()).asFiles
            val androidDependencies = listOf(
                ".*/com\\.android\\..*/.*".toRegex(),
                ".*/androidx\\..*/.*".toRegex(),
                ".*/com\\.google\\..*/.*".toRegex(),
            )
            val noAndroid = classpath.filterNot { dependency -> androidDependencies.any { it.matches(dependency.path) } }

            return defaultRunner(*args)
                .withPluginClasspath(noAndroid)
        }
    }
}

private fun WithGradleTest.defaultRunner(vararg args: String) =
    GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments(args.toList() + "--stacktrace")
        .forwardOutput()
