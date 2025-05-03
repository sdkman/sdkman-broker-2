package io.sdkman.broker.support

import arrow.core.Option
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot

// Custom matchers for Arrow Option types
// Provides expressive, readable assertions for functional tests

// Matcher for Option.isSome() with optional value validation
fun <T> beSome(expectedValue: T? = null) =
    object : Matcher<Option<T>> {
        override fun test(value: Option<T>): MatcherResult {
            val isSome = value.isSome()
            val valueMatches = expectedValue?.let { value.getOrNull() == it } ?: true

            return MatcherResult(
                isSome && valueMatches,
                { "Expected Option to be Some${expectedValue?.let { " with value $it" } ?: ""}, but was $value" },
                { "Expected Option not to be Some${expectedValue?.let { " with value $it" } ?: ""}, but was $value" }
            )
        }
    }

// Matcher for Option.isNone()
fun <T> beNone() =
    object : Matcher<Option<T>> {
        override fun test(value: Option<T>): MatcherResult {
            return MatcherResult(
                value.isNone(),
                { "Expected Option to be None, but was $value" },
                { "Expected Option not to be None, but was None" }
            )
        }
    }

// Extension functions for more readable syntax
infix fun <T> Option<T>.shouldBeSome(expected: T) = this should beSome(expected)

fun <T> Option<T>.shouldBeSome() = this should beSome()

fun <T> Option<T>.shouldBeNone() = this should beNone()

infix fun <T> Option<T>.shouldNotBeSome(expected: T) = this shouldNot beSome(expected)

fun <T> Option<T>.shouldNotBeSome() = this shouldNot beSome()

fun <T> Option<T>.shouldNotBeNone() = this shouldNot beNone()
