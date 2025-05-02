package io.sdkman.broker.support

import arrow.core.Either
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot

fun <L, R> beRight(expected: R): Matcher<Either<L, R>> =
    object : Matcher<Either<L, R>> {
        override fun test(value: Either<L, R>): MatcherResult {
            return when (value) {
                is Either.Right ->
                    MatcherResult(
                        value.value == expected,
                        { "Expected Either.Right($expected) but was Either.Right(${value.value})" },
                        { "Expected Either to not be Right($expected)" }
                    )
                is Either.Left ->
                    MatcherResult(
                        false,
                        { "Expected Either.Right($expected) but was Either.Left(${value.value})" },
                        { "Expected Either to not be Right($expected)" }
                    )
            }
        }
    }

fun <L, R> beRightAnd(predicate: (R) -> Boolean): Matcher<Either<L, R>> =
    object : Matcher<Either<L, R>> {
        override fun test(value: Either<L, R>): MatcherResult {
            return when (value) {
                is Either.Right ->
                    MatcherResult(
                        predicate(value.value),
                        { "Expected Either.Right to satisfy predicate but was Either.Right(${value.value})" },
                        { "Expected Either.Right not to satisfy predicate" }
                    )
                is Either.Left ->
                    MatcherResult(
                        false,
                        { "Expected Either.Right but was Either.Left(${value.value})" },
                        { "Expected Either to not be Right" }
                    )
            }
        }
    }

fun <L, R> beLeft(expected: L): Matcher<Either<L, R>> =
    object : Matcher<Either<L, R>> {
        override fun test(value: Either<L, R>): MatcherResult {
            return when (value) {
                is Either.Left ->
                    MatcherResult(
                        value.value == expected,
                        { "Expected Either.Left($expected) but was Either.Left(${value.value})" },
                        { "Expected Either to not be Left($expected)" }
                    )
                is Either.Right ->
                    MatcherResult(
                        false,
                        { "Expected Either.Left($expected) but was Either.Right(${value.value})" },
                        { "Expected Either to not be Left($expected)" }
                    )
            }
        }
    }

fun <L, R> beLeftAnd(predicate: (L) -> Boolean): Matcher<Either<L, R>> =
    object : Matcher<Either<L, R>> {
        override fun test(value: Either<L, R>): MatcherResult {
            return when (value) {
                is Either.Left ->
                    MatcherResult(
                        predicate(value.value),
                        { "Expected Either.Left to satisfy predicate but was Either.Left(${value.value})" },
                        { "Expected Either.Left not to satisfy predicate" }
                    )
                is Either.Right ->
                    MatcherResult(
                        false,
                        { "Expected Either.Left but was Either.Right(${value.value})" },
                        { "Expected Either to not be Left" }
                    )
            }
        }
    }

infix fun <L, R> Either<L, R>.shouldBeRight(expected: R) = this should beRight(expected)

infix fun <L, R> Either<L, R>.shouldNotBeRight(expected: R) = this shouldNot beRight(expected)

infix fun <L, R> Either<L, R>.shouldBeLeft(expected: L) = this should beLeft(expected)

infix fun <L, R> Either<L, R>.shouldNotBeLeft(expected: L) = this shouldNot beLeft(expected)

// Additional extension functions for predicate-based assertions
infix fun <L, R> Either<L, R>.shouldBeRightAnd(predicate: (R) -> Boolean) = this should beRightAnd(predicate)

infix fun <L, R> Either<L, R>.shouldBeLeftAnd(predicate: (L) -> Boolean) = this should beLeftAnd(predicate)
