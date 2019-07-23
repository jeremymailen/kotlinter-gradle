package org.jmailen.gradle.kotlinter.support

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import org.jmailen.gradle.kotlinter.tasks.format.FormatExecutionContext
import org.jmailen.gradle.kotlinter.tasks.lint.LintExecutionContext

/**
 * Global repository storing ExecutionContext that needs to be available in Worker Runnable.
 */
class ExecutionContextRepository<ExecutionContextType : ExecutionContext> {

    companion object {
        /**
         * Instance for the linting task.
         */
        val lintInstance = ExecutionContextRepository<LintExecutionContext>()
        /**
         * Instance for the formatting task.
         */
        val formatInstance = ExecutionContextRepository<FormatExecutionContext>()
    }

    private val executionContextById: ConcurrentMap<UUID, ExecutionContextType> = ConcurrentHashMap<UUID, ExecutionContextType>()

    fun register(executionContext: ExecutionContextType): UUID {
        val id = UUID.randomUUID()
        executionContextById[id] = executionContext
        return id
    }

    fun get(id: UUID): ExecutionContextType =
        executionContextById.getValue(id)

    fun unregister(id: UUID) {
        executionContextById.remove(id)
    }
}
