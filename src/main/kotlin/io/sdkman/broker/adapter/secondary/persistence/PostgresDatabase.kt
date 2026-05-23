package io.sdkman.broker.adapter.secondary.persistence

import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import java.sql.Connection
import javax.sql.DataSource

/**
 * Builds the single shared Exposed [Database] used by every Postgres repository.
 *
 * Pinning [Connection.TRANSACTION_READ_COMMITTED] makes the isolation level
 * deterministic instead of driver-default and stops Exposed probing the database
 * for its default on the first transaction (spec Business Rule 5).
 */
object PostgresDatabase {
    fun connect(dataSource: DataSource): Database =
        Database.connect(
            datasource = dataSource,
            databaseConfig = DatabaseConfig { defaultIsolationLevel = Connection.TRANSACTION_READ_COMMITTED }
        )
}
