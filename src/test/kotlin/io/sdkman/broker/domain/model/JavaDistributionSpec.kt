package io.sdkman.broker.domain.model

import arrow.core.None
import arrow.core.Some
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe

class JavaDistributionSpec :
    ShouldSpec({

        context("JavaDistribution.resolve") {

            should("strip the suffix and return the full enum name for every known short code") {
                forAll(
                    row("17.0.2-tem", "17.0.2", "TEMURIN"),
                    row("17.0.2-amzn", "17.0.2", "CORRETTO"),
                    row("17.0.2-zulu", "17.0.2", "ZULU"),
                    row("17.0.2-librca", "17.0.2", "LIBERICA"),
                    row("17.0.2-nik", "17.0.2", "LIBERICA_NIK"),
                    row("17.0.2-oracle", "17.0.2", "ORACLE"),
                    row("17.0.2-open", "17.0.2", "OPENJDK"),
                    row("17.0.2-graal", "17.0.2", "GRAALVM"),
                    row("17.0.2-graalce", "17.0.2", "GRAALCE"),
                    row("17.0.2-mandrel", "17.0.2", "MANDREL"),
                    row("17.0.2-ms", "17.0.2", "MICROSOFT"),
                    row("17.0.2-sapmchn", "17.0.2", "SAP_MACHINE"),
                    row("17.0.2-sem", "17.0.2", "SEMERU"),
                    row("17.0.2-kona", "17.0.2", "KONA"),
                    row("17.0.2-bisheng", "17.0.2", "BISHENG"),
                    row("17.0.2-jbr", "17.0.2", "JETBRAINS")
                ) { token, expectedVersion, expectedEnum ->
                    JavaDistribution.resolve(token) shouldBe
                        JavaDistribution.Resolution(expectedVersion, Some(expectedEnum))
                }
            }

            should("split on the last hyphen for multi-hyphen tokens") {
                JavaDistribution.resolve("17.0.2.1-2-tem") shouldBe
                    JavaDistribution.Resolution("17.0.2.1-2", Some("TEMURIN"))
            }

            should("pass through verbatim with None when the suffix is not a known short code") {
                JavaDistribution.resolve("17.0.2-bogus") shouldBe
                    JavaDistribution.Resolution("17.0.2-bogus", None)
            }

            should("pass through verbatim with None when there is no hyphen at all") {
                JavaDistribution.resolve("17") shouldBe
                    JavaDistribution.Resolution("17", None)
            }
        }

        context("JavaDistribution.shortCodeFor") {

            should("reverse-map every full enum name back to its short code") {
                forAll(
                    row("TEMURIN", "tem"),
                    row("CORRETTO", "amzn"),
                    row("ZULU", "zulu"),
                    row("LIBERICA", "librca"),
                    row("LIBERICA_NIK", "nik"),
                    row("ORACLE", "oracle"),
                    row("OPENJDK", "open"),
                    row("GRAALVM", "graal"),
                    row("GRAALCE", "graalce"),
                    row("MANDREL", "mandrel"),
                    row("MICROSOFT", "ms"),
                    row("SAP_MACHINE", "sapmchn"),
                    row("SEMERU", "sem"),
                    row("KONA", "kona"),
                    row("BISHENG", "bisheng"),
                    row("JETBRAINS", "jbr")
                ) { enumName, expectedShortCode ->
                    JavaDistribution.shortCodeFor(enumName) shouldBe Some(expectedShortCode)
                }
            }

            should("return None for an unknown enum name") {
                JavaDistribution.shortCodeFor("DRAGONWELL") shouldBe None
            }
        }

        context("JavaDistribution.shortCodeToEnum") {

            should("expose exactly the 16 supported distributions") {
                JavaDistribution.shortCodeToEnum.size shouldBe 16
            }
        }
    })
