package io.sdkman.broker.adapter.secondary.persistence

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.sdkman.broker.support.PostgresTestListener
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Tag
import java.sql.Connection

@Tag("integration")
class PostgresDatabaseIntegrationSpec :
    ShouldSpec({
        register(PostgresTestListener)

        should("pin READ_COMMITTED as the default transaction isolation level") {
            // given: a Database built through the shared factory
            val database = PostgresDatabase.connect(PostgresTestListener.dataSource)

            // when: a transaction runs without an explicit isolation level
            val isolationLevel = transaction(database) { transactionIsolation }

            // then: the pinned isolation level is in effect (spec Business Rule 5)
            isolationLevel shouldBe Connection.TRANSACTION_READ_COMMITTED
        }
    })
