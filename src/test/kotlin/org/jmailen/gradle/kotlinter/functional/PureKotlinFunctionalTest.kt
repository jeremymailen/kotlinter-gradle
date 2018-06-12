package org.jmailen.gradle.kotlinter.functional

import com.mkobit.gradle.test.assertj.GradleAssertions.assertThat
import com.mkobit.gradle.test.kotlin.testkit.runner.build
import com.mkobit.gradle.test.kotlin.testkit.runner.buildAndFail
import com.mkobit.gradle.test.kotlin.testkit.runner.info
import com.mkobit.gradle.test.kotlin.testkit.runner.profile
import com.mkobit.gradle.test.kotlin.testkit.runner.setupProjectDir
import com.mkobit.gradle.test.kotlin.testkit.runner.stacktrace
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.After
import org.junit.Before
import org.junit.Test

class PureKotlinFunctionalTest {

    private lateinit var gradleRunner: GradleRunner

    @Before
    fun setUp() {
        gradleRunner = GradleRunner.create().apply {
            withPluginClasspath()
            withProjectDir(createTempDir(prefix = "kotlinter"))
            withArguments()
             info = true
             stacktrace = true
             profile = true
            setupProjectDir {
                "build.gradle"(content = plugin) {
                    append(System.lineSeparator())
                }
                "settings.gradle"(content = "rootProject.name = 'pure-kotlin-example'${System.lineSeparator()}")
            }
        }
    }

    @After
    fun tearDown() {
        gradleRunner.projectDir.deleteRecursively()
    }

    @Test
    fun `basic use case with correct formatting`() {
        gradleRunner.setupProjectDir {
            "src/main/java/com/example/" {
                "Main.kt"(content = """
                    package com.example

                    fun main(args: Array<String>) {
                        println("Hello, world!")
                    }
                """.trimIndent())
            }
        }


        val result = gradleRunner.build("check")
        result.task(":lintKotlin")!!
        result.tasks.find { it.path == ":lintKotlin" }!!.outcome == TaskOutcome.SUCCESS

        assertThat(result)
                .hasTaskSuccessAtPath(":lintKotlin")
                .hasTaskSuccessAtPath(":lintKotlinMain")
    }

    @Test
    fun `basic use case with incorrect formatting`() {
        gradleRunner.setupProjectDir {
            "src/main/java/com/example/" {
                "Main.kt"(content = """
                    package com.example

                                fun main(args: Array< String>
                    ) {
                        println("Hello, world!")
                    }
                """.trimIndent())
            }
        }


        val result = gradleRunner.buildAndFail("check")

        assertThat(result)
                .hasTaskFailedAtPath(":lintKotlinMain")
    }

    companion object {
        val plugin = """
            plugins {
                id "org.jmailen.kotlinter"
                id "org.jetbrains.kotlin.jvm"
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                compile "org.jetbrains.kotlin:kotlin-stdlib"
            }
        """.trimIndent()
    }

}