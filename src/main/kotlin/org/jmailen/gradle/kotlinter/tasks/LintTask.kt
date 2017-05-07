package org.jmailen.gradle.kotlinter.tasks

import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction

class LintTask : SourceTask() {

    @TaskAction
    fun run() {
        this.getSource().forEach { file ->
            println("lint: $file")
        }
    }
}
