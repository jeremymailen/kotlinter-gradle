package org.jmailen.gradle.kotlinter.support

import com.nhaarman.mockitokotlin2.mock
import org.junit.Assert.assertEquals
import org.junit.Test

class ExecutionContextRepositoryTest {

    @Test
    fun retrievingRegisteredContextWorks() {
        val repository = ExecutionContextRepository.instance
        val executionContext = ExecutionContext(emptyList(), mock())
        val id = repository.registerExecutionContext(executionContext)

        val result = repository.retrieveExecutionContext(id)

        assertEquals(executionContext, result)
    }

    @Test(expected = NoSuchElementException::class)
    fun retrievingUnregisteredContextFails() {
        val repository = ExecutionContextRepository.instance
        val executionContext = ExecutionContext(emptyList(), mock())
        val id = repository.registerExecutionContext(executionContext)
        repository.unregisterExecutionContext(id)

        repository.retrieveExecutionContext(id)
    }
}