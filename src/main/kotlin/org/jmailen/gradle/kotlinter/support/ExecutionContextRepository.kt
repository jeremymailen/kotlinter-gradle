package org.jmailen.gradle.kotlinter.support

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * Global repository storing ExecutionContext that needs to be available in Worker Runnable.
 */
class ExecutionContextRepository {

    companion object {
        val instance = ExecutionContextRepository()
    }

    private val executionContextById: ConcurrentMap<UUID, ExecutionContext> = ConcurrentHashMap<UUID, ExecutionContext>()

    fun registerExecutionContext(executionContext: ExecutionContext): UUID {
        val id = UUID.randomUUID()
        executionContextById[id] = executionContext
        return id
    }

    fun retrieveExecutionContext(id: UUID): ExecutionContext =
        executionContextById.getValue(id)

    fun unregisterExecutionContext(id: UUID) {
        executionContextById.remove(id)
    }
}