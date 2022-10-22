package org.jmailen.gradle.kotlinter.functional

import org.gradle.testkit.runner.TaskOutcome
import org.jmailen.gradle.kotlinter.functional.utils.kotlinClass
import org.jmailen.gradle.kotlinter.functional.utils.resolve
import org.jmailen.gradle.kotlinter.functional.utils.settingsFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import java.io.File

class ReportersTest : WithGradleTest.Kotlin() {

    lateinit var projectRoot: File

    @BeforeEach
    fun setUp() {
        projectRoot = testProjectDir.apply {
            resolve("settings.gradle") { writeText(settingsFile) }
            resolve("build.gradle") {
                // language=groovy
                val buildScript =
                    """
                    plugins {
                        id 'kotlin'
                        id 'org.jmailen.kotlinter'
                    }
                    
                    repositories {
                        mavenCentral()
                    }
                
                    """.trimIndent()
                writeText(buildScript)
            }
        }
        projectRoot.resolve("src/main/kotlin/CustomClass.kt") {
            writeText(kotlinClass("CustomClass"))
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS, disabledReason = "Report file content differs on Windows due to different path separator")
    fun `supports all types of reporters`() {
        projectRoot.resolve("build.gradle") {
            // language=groovy
            val buildScript =
                """

                kotlinter {
                    reporters = [
                        'checkstyle',
                        'html',
                        'json',
                        'plain',
                        'sarif',
                    ] 
                }

                """
            appendText(buildScript)
        }

        build("lintKotlin").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":lintKotlin")?.outcome)
            assertEquals(expectedEmptyPlain(), reportContent("/main-lint.txt"))
            assertEquals(expectedEmptyCheckstyle(), reportContent("main-lint.xml"))
            assertEquals(expectedEmptyHtml(), reportContent("/main-lint.html"))
            assertEquals(expectedEmptyJson(), reportContent("/main-lint.json"))
            assertTrue(reportContent("/main-lint.sarif.json").isNotBlank())
        }
        build("lintKotlin").apply {
            assertEquals(TaskOutcome.UP_TO_DATE, task(":lintKotlin")?.outcome)
        }

        projectRoot.resolve("src/main/kotlin/FirstClass.kt") {
            writeText(kotlinClass("WrongClassName"))
        }
        projectRoot.resolve("src/main/kotlin/SecondClass.kt") {
            writeText(kotlinClass("MultipleOffencesInSingleSourceSet"))
        }

        projectRoot.resolve("src/test/kotlin/CustomTestClass.kt") {
            writeText(kotlinClass("DifferentSourceSet"))
        }

        buildAndFail("lintKotlin").apply {
            assertEquals(TaskOutcome.FAILED, task(":lintKotlinMain")?.outcome)
            assertEquals(expectedFailedPlain(), reportContent("/main-lint.txt"))
            assertEquals(expectedFailedCheckstyle(), reportContent("/main-lint.xml"))
            assertEquals(expectedFailedHtml(), reportContent("/main-lint.html"))
            assertEquals(expectedFailedJson(), reportContent("/main-lint.json"))
            assertTrue(reportContent("/main-lint.sarif.json").isNotBlank())
        }
    }

    @Test
    fun `uses reporters from overridden ktlint version`() {
        projectRoot.resolve("build.gradle") {
            // language=groovy
            val buildScript =
                """

                kotlinter {
                    reporters = ['sarif']
                    ktlintVersion = "0.47.0"
                }

                """
            appendText(buildScript)
        }

        build("lintKotlin").apply {
            val reportContent = projectRoot.resolve("build/reports/ktlint/main-lint.sarif.json").readText()
            assertTrue(reportContent.contains(""""version": "0.47.0""""))
            assertTrue(reportContent.contains(""""semanticVersion": "0.47.0""""))
        }
    }

    private fun reportContent(reportName: String) = projectRoot.resolve("build/reports/ktlint/$reportName").readText()
}

private fun expectedEmptyPlain() = ""

// language=xml
private fun expectedEmptyCheckstyle() = """
<?xml version="1.0" encoding="utf-8"?>
<checkstyle version="8.0">
</checkstyle>

""".trimIndent()

// language=html
private fun expectedEmptyHtml() = """
<html>
<head>
<link href="https://fonts.googleapis.com/css?family=Source+Code+Pro" rel="stylesheet" />
<meta http-equiv="Content-Type" Content="text/html; Charset=UTF-8">
<style>
body {
    font-family: 'Source Code Pro', monospace;
}
h3 {
    font-size: 12pt;
}</style>
</head>
<body>
<p>Congratulations, no issues found!</p>
</body>
</html>

""".trimIndent()

// language=json
private fun expectedEmptyJson() = """
[
]

""".trimIndent()

private fun expectedFailedPlain() = """
src/main/kotlin/FirstClass.kt:1:1: File 'FirstClass.kt' contains a single top level declaration and should be named 'WrongClassName.kt' (filename)
src/main/kotlin/SecondClass.kt:1:1: File 'SecondClass.kt' contains a single top level declaration and should be named 'MultipleOffencesInSingleSourceSet.kt' (filename)

""".trimIndent()

// language=xml
private fun expectedFailedCheckstyle() = """
<?xml version="1.0" encoding="utf-8"?>
<checkstyle version="8.0">
    <file name="src/main/kotlin/FirstClass.kt">
        <error line="1" column="1" severity="error" message="File &apos;FirstClass.kt&apos; contains a single top level declaration and should be named &apos;WrongClassName.kt&apos;" source="filename" />
    </file>
    <file name="src/main/kotlin/SecondClass.kt">
        <error line="1" column="1" severity="error" message="File &apos;SecondClass.kt&apos; contains a single top level declaration and should be named &apos;MultipleOffencesInSingleSourceSet.kt&apos;" source="filename" />
    </file>
</checkstyle>

""".trimIndent()

// language=html
private fun expectedFailedHtml() = """
<html>
<head>
<link href="https://fonts.googleapis.com/css?family=Source+Code+Pro" rel="stylesheet" />
<meta http-equiv="Content-Type" Content="text/html; Charset=UTF-8">
<style>
body {
    font-family: 'Source Code Pro', monospace;
}
h3 {
    font-size: 12pt;
}</style>
</head>
<body>
<h1>Overview</h1>
<p>Issues found: 2</p>
<p>Issues corrected: 0</p>
<h3>src/main/kotlin/FirstClass.kt</h3>
<ul>
<li>(1, 1): File &apos;FirstClass.kt&apos; contains a single top level declaration and should be named &apos;WrongClassName.kt&apos;  (filename)</li>
</ul>
<h3>src/main/kotlin/SecondClass.kt</h3>
<ul>
<li>(1, 1): File &apos;SecondClass.kt&apos; contains a single top level declaration and should be named &apos;MultipleOffencesInSingleSourceSet.kt&apos;  (filename)</li>
</ul>
</body>
</html>

""".trimIndent()

// language=json
fun expectedFailedJson() = """
[
    {
        "file": "src/main/kotlin/FirstClass.kt",
        "errors": [
            {
                "line": 1,
                "column": 1,
                "message": "File 'FirstClass.kt' contains a single top level declaration and should be named 'WrongClassName.kt'",
                "rule": "filename"
            }
        ]
    },
    {
        "file": "src/main/kotlin/SecondClass.kt",
        "errors": [
            {
                "line": 1,
                "column": 1,
                "message": "File 'SecondClass.kt' contains a single top level declaration and should be named 'MultipleOffencesInSingleSourceSet.kt'",
                "rule": "filename"
            }
        ]
    }
]

""".trimIndent()
