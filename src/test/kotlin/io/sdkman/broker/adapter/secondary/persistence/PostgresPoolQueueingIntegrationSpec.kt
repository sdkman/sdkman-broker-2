package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.some
import arrow.core.toOption
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.sdkman.broker.config.AppConfig
import io.sdkman.broker.config.PersistenceBackend
import io.sdkman.broker.support.PostgresTestListener
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Tag
import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * Proves the HikariCP pool is the active path: when more concurrent transactions
 * are launched than `maximumPoolSize`, the surplus requests *queue* and are served
 * as connections free up, rather than being refused. This is the behavioural
 * guarantee behind the externalised pool tuning (spec acceptance criterion:
 * "opens more concurrent queries than a small configured maxSize and asserts they
 * all complete"). Asserting on the live `HikariPoolMXBean` rather than wall-clock
 * timing keeps the test deterministic.
 */
@Tag("integration")
class PostgresPoolQueueingIntegrationSpec :
    ShouldSpec({
        listener(PostgresTestListener)

        val maxPoolSize = 2
        val concurrentRequests = 8

        val connectivity = PostgresConnectivity(poolConfig(maxPoolSize))
        val dataSource = connectivity.dataSource()
        val database = PostgresDatabase.connect(dataSource)

        // The pool name is fixed to "sdkman-broker-pool" by production code; without
        // JMX MBean registration (Hikari default) a shared name is benign, but we still
        // close the pool here so it cannot leak into other specs in the same JVM.
        afterSpec { dataSource.close() }

        should("queue requests above maxSize and serve them all as connections free up") {
            // given: the pool is initialised (so its MXBean is observable) and a sampler
            // tracking the peak active connections and peak waiting threads
            transaction(database) { exec("SELECT 1") }

            val sampling = AtomicBoolean(true)
            val peakActiveConnections = AtomicInteger(0)
            val peakThreadsAwaiting = AtomicInteger(0)
            val sampler =
                thread {
                    while (sampling.get()) {
                        dataSource.hikariPoolMXBean.toOption().onSome { pool ->
                            peakActiveConnections.accumulateAndGet(pool.activeConnections) { a, b -> maxOf(a, b) }
                            peakThreadsAwaiting.accumulateAndGet(pool.threadsAwaitingConnection) { a, b -> maxOf(a, b) }
                        }
                        Thread.sleep(SAMPLING_INTERVAL_MS)
                    }
                }

            // when: more concurrent transactions than the pool can serve at once each hold
            // a connection for a short blocking query, released together via a barrier
            val barrier = CyclicBarrier(concurrentRequests)
            val executor = Executors.newFixedThreadPool(concurrentRequests)
            val tasks =
                (1..concurrentRequests).map {
                    Callable {
                        Either.catch {
                            barrier.await()
                            transaction(database) { exec("SELECT pg_sleep($HOLD_SECONDS)") }
                        }
                    }
                }
            val results = executor.invokeAll(tasks).map { it.get() }
            executor.shutdown()
            sampling.set(false)
            sampler.join()

            // then: every queued request completed successfully...
            withClue("all $concurrentRequests requests should complete; the pool queues rather than refuses") {
                results.all { it.isRight() } shouldBe true
            }
            // ...active connections never exceeded the cap, peaking exactly at maxSize...
            withClue("active connections should saturate at maxSize=$maxPoolSize, proving the pool is the limiter") {
                peakActiveConnections.get() shouldBe maxPoolSize
            }
            // ...and the surplus demand was forced to wait, proving the requests queued
            withClue("more requests than maxSize means at least one thread must wait for a connection") {
                (peakThreadsAwaiting.get() >= 1) shouldBe true
            }
        }
    })

private const val HOLD_SECONDS = 0.2
private const val SAMPLING_INTERVAL_MS = 5L

private fun poolConfig(maxPoolSize: Int): AppConfig =
    object : AppConfig {
        override val mongodbHost: String = "localhost"
        override val mongodbPort: String = "27017"
        override val mongodbDatabase: String = "sdkman"
        override val mongodbUsername: Option<String> = None
        override val mongodbPassword: Option<String> = None
        override val mongodbAuthMechanism: Option<String> = None
        override val postgresHost: String = PostgresTestListener.host
        override val postgresPort: String = PostgresTestListener.port.toString()
        override val postgresDatabase: String = PostgresTestListener.databaseName
        override val postgresUsername: Option<String> = PostgresTestListener.username.some()
        override val postgresPassword: Option<String> = PostgresTestListener.password.some()
        override val postgresSslMode: String = "disable"
        override val postgresPoolMaxSize: Int = maxPoolSize
        override val postgresPoolMinIdle: Int = 1
        override val postgresPoolConnectionTimeoutMs: Long = 5_000L
        override val postgresPoolMaxLifetimeMs: Long = 1_800_000L
        override val postgresPoolIdleTimeoutMs: Long = 600_000L
        override val serverPort: Int = 8080
        override val serverHost: String = "127.0.0.1"
        override val persistenceBackend: PersistenceBackend = PersistenceBackend.Mongo
    }
