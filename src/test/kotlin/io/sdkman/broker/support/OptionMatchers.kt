package io.sdkman.broker.support

import arrow.core.Option
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot

// Custom matchers for Arrow Option types
// Provides expressive, readable assertions for functional tests

// Matcher for Option.isSome() without value validation
fun <T> beSome(): Matcher<Option<T>> =
    object : Matcher<Option<T>> {
        override fun test(value: Option<T>): MatcherResult =
            MatcherResult(
                value.isSome(),
                { "Expected Option to be Some, but was $value" },
                { "Expected Option not to be Some, but was $value" }
            )
    }

// Matcher for Option.isSome() with value validation
fun <T> beSome(expectedValue: T): Matcher<Option<T>> =
    object : Matcher<Option<T>> {
        override fun test(value: Option<T>): MatcherResult =
            MatcherResult(
                value.isSome() && value.getOrNull() == expectedValue,
                { "Expected Option to be Some with value $expectedValue, but was $value" },
                { "Expected Option not to be Some with value $expectedValue, but was $value" }
            )
    }

// Matcher for Option.isNone()
fun <T> beNone(): Matcher<Option<T>> =
    object : Matcher<Option<T>> {
        override fun test(value: Option<T>): MatcherResult =
            MatcherResult(
                value.isNone(),
                { "Expected Option to be None, but was $value" },
                { "Expected Option not to be None, but was None" }
            )
    }

// Extension functions for more readable syntax
infix fun <T> Option<T>.shouldBeSome(expected: T) = this should beSome(expected)

fun <T> Option<T>.shouldBeSome() = this should beSome()

fun <T> Option<T>.shouldBeNone() = this should beNone()

infix fun <T> Option<T>.shouldNotBeSome(expected: T) = this shouldNot beSome(expected)

fun <T> Option<T>.shouldNotBeSome() = this shouldNot beSome()

fun <T> Option<T>.shouldNotBeNone() = this shouldNot beNone()

// Extension function for asserting Some with additional validation
infix fun <T> Option<T>.shouldBeSomeAnd(assertion: (T) -> Unit) {
    this.shouldBeSome()
    this.map(assertion)
}
