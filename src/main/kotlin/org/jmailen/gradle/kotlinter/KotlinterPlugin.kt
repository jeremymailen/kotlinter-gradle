package org.jmailen.gradle.kotlinter

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jmailen.gradle.kotlinter.tasks.LintTask

class KotlinterPlugin : Plugin<Project> {

    override fun apply(project: Project?) {
        project?.let { p ->
            createTasks(p)
        }
    }

    fun createTasks(project: Project) {
        // compileKotlin
        // compileTestKotlin
        project.tasks.create("lintKotlin", LintTask::class.java)
    }
}
