package tech.testsys.infra.database.api.transaction

import jakarta.persistence.OptimisticLockException
import org.hibernate.StaleStateException
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import java.sql.SQLException

class TransactionConflictsTests {

    @ParameterizedTest
    @ValueSource(strings = ["40001", "40P01", "23505"])
    fun `should treat a serialization failure, a deadlock and a unique key violation as a conflict`(sqlState: String) {
        val failure = DataIntegrityViolationException("write failed", SQLException("rejected", sqlState))

        assertTrue(TransactionConflicts.isConflict(failure))
    }

    @ParameterizedTest
    @ValueSource(strings = ["23503", "23502"])
    fun `should not treat a foreign key or not null violation as a conflict`(sqlState: String) {
        val failure = DataIntegrityViolationException("write failed", SQLException("rejected", sqlState))

        assertFalse(TransactionConflicts.isConflict(failure))
    }

    @Test
    fun `should treat a concurrency failure of spring as a conflict`() {
        assertTrue(TransactionConflicts.isConflict(CannotAcquireLockException("locked")))
    }

    @Test
    fun `should treat an optimistic lock failure of jpa as a conflict`() {
        assertTrue(TransactionConflicts.isConflict(OptimisticLockException("stale")))
    }

    @Test
    fun `should treat a stale state of hibernate as a conflict`() {
        assertTrue(TransactionConflicts.isConflict(StaleStateException("stale")))
    }

    @Test
    fun `should find a conflict deep in the cause chain`() {
        val conflict = ObjectOptimisticLockingFailureException("Task", 1L)
        val failure = IllegalStateException("outer", RuntimeException("middle", conflict))

        assertTrue(TransactionConflicts.isConflict(failure))
    }

    @Test
    fun `should not treat an unrelated failure as a conflict`() {
        assertFalse(TransactionConflicts.isConflict(IllegalStateException("broken", IllegalArgumentException("bad"))))
    }
}
