package io.sdkman.broker.acceptance

import arrow.core.Either
import arrow.core.left
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import io.sdkman.broker.adapter.primary.rest.INTERNAL_ERROR_MESSAGE
import io.sdkman.broker.domain.model.VersionTagError
import io.sdkman.broker.domain.service.VersionTagService
import io.sdkman.broker.support.MongoTestListener
import io.sdkman.broker.support.PostgresTestListener
import io.sdkman.broker.support.shouldBeEmptyUncacheableError
import io.sdkman.broker.support.whenRequested
import org.junit.jupiter.api.Tag
import org.slf4j.LoggerFactory

private const val VERSION_ROUTES_LOGGER = "io.sdkman.broker.adapter.primary.rest.VersionRoutes"
private const val APP_LOGGER = "io.sdkman.broker.App"

private fun failingService(error: VersionTagError): VersionTagService =
    object : VersionTagService {
        override fun findVersionByTag(
            candidate: String,
            tag: String,
            platformCode: String
        ): Either<VersionTagError, String> = error.left()
    }

private fun throwingService(cause: Throwable): VersionTagService =
    object : VersionTagService {
        override fun findVersionByTag(
            candidate: String,
            tag: String,
            platformCode: String
        ): Either<VersionTagError, String> = throw cause
    }

@Tag("acceptance")
class VersionByTagErrorAcceptanceSpec :
    ShouldSpec({
        extension(MongoTestListener)
        extension(PostgresTestListener)

        val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        val logEvents = ListAppender<ILoggingEvent>()

        fun logged(
            loggerName: String,
            level: Level
        ): List<ILoggingEvent> = logEvents.list.filter { it.loggerName == loggerName && it.level == level }

        beforeSpec {
            logEvents.start()
            rootLogger.addAppender(logEvents)
        }
        afterSpec {
            rootLogger.detachAppender(logEvents)
            logEvents.stop()
        }
        beforeTest { logEvents.list.clear() }

        should("answer a database error with an empty 500 and no-store") {
            // given: the lookup fails in the database
            val service = failingService(VersionTagError.DatabaseError(RuntimeException("permission denied")))

            // when: a tag is requested
            whenRequested("/version/sdkcli/stable/universal", versionTagService = service) { response ->
                // then: the 500 has no body, no content type and is not cacheable
                response.shouldBeEmptyUncacheableError(HttpStatusCode.InternalServerError)
            }
        }

        should("answer an exception escaping the handler with an empty 500 and no-store") {
            // given: the lookup throws instead of returning an error
            val service = throwingService(IllegalStateException("boom"))

            // when: a tag is requested
            whenRequested("/version/sdkcli/stable/universal", versionTagService = service) { response ->
                // then: the 500 has no body, no content type and is not cacheable
                response.shouldBeEmptyUncacheableError(HttpStatusCode.InternalServerError)
            }
        }

        should("log an unknown platform at WARN as INVALID_PLATFORM") {
            // given: the lookup rejects the platform
            val service = failingService(VersionTagError.InvalidPlatform("bogus"))

            // when: a tag is requested
            whenRequested("/version/sdkcli/stable/bogus", versionTagService = service) {
                // then: one WARN names the code and the platform
                logged(VERSION_ROUTES_LOGGER, Level.WARN).map { it.formattedMessage } shouldBe
                    listOf("INVALID_PLATFORM: Invalid platform 'bogus'")
            }
        }

        should("log an unknown tag at WARN as TAG_NOT_FOUND") {
            // given: the lookup finds no row for the tag
            val service = failingService(VersionTagError.TagNotFound("sdkcli", "nightly", "UNIVERSAL"))

            // when: the tag is requested
            whenRequested("/version/sdkcli/nightly/universal", versionTagService = service) {
                // then: one WARN names the code, candidate, tag and platform
                logged(VERSION_ROUTES_LOGGER, Level.WARN).map { it.formattedMessage } shouldBe
                    listOf("TAG_NOT_FOUND: No sdkcli tag 'nightly' available for platform 'UNIVERSAL'")
            }
        }

        should("log a database error at ERROR with its cause") {
            // given: the lookup fails in the database
            val service = failingService(VersionTagError.DatabaseError(RuntimeException("permission denied")))

            // when: a tag is requested
            whenRequested("/version/sdkcli/stable/universal", versionTagService = service) {
                // then: one ERROR carries the generic message and the cause
                val errors = logged(VERSION_ROUTES_LOGGER, Level.ERROR)
                errors.map { it.formattedMessage } shouldBe listOf(INTERNAL_ERROR_MESSAGE)
                errors.map { it.throwableProxy.message } shouldBe listOf("permission denied")
            }
        }

        should("log an exception escaping the handler at ERROR with its stack trace") {
            // given: the lookup throws instead of returning an error
            val service = throwingService(IllegalStateException("boom"))

            // when: a tag is requested
            whenRequested("/version/sdkcli/stable/universal", versionTagService = service) {
                // then: one ERROR carries the generic message and the thrown exception
                val errors = logged(APP_LOGGER, Level.ERROR)
                errors.map { it.formattedMessage } shouldBe listOf(INTERNAL_ERROR_MESSAGE)
                errors.map { it.throwableProxy.message } shouldBe listOf("boom")
            }
        }
    })
