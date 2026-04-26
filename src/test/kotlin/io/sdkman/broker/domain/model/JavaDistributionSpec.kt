package io.sdkman.broker.domain.model

import arrow.core.None
import arrow.core.Some
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class JavaDistributionSpec :
    ShouldSpec({
        context("JavaDistribution.fromShortCode") {

            should("resolve every known short code to its distribution") {
                val expected =
                    mapOf(
                        "tem" to JavaDistribution.TEMURIN,
                        "amzn" to JavaDistribution.CORRETTO,
                        "zulu" to JavaDistribution.ZULU,
                        "librca" to JavaDistribution.LIBERICA,
                        "nik" to JavaDistribution.LIBERICA_NIK,
                        "oracle" to JavaDistribution.ORACLE,
                        "open" to JavaDistribution.OPENJDK,
                        "graal" to JavaDistribution.GRAALVM,
                        "graalce" to JavaDistribution.GRAALCE,
                        "mandrel" to JavaDistribution.MANDREL,
                        "ms" to JavaDistribution.MICROSOFT,
                        "sapmchn" to JavaDistribution.SAP_MACHINE,
                        "sem" to JavaDistribution.SEMERU,
                        "kona" to JavaDistribution.KONA,
                        "bisheng" to JavaDistribution.BISHENG,
                        "jbr" to JavaDistribution.JETBRAINS
                    )
                expected.forEach { (shortCode, distribution) ->
                    JavaDistribution.fromShortCode(shortCode) shouldBe Some(distribution)
                }
            }

            should("expose the full enum name as the canonical name") {
                JavaDistribution.TEMURIN.name shouldBe "TEMURIN"
                JavaDistribution.LIBERICA_NIK.name shouldBe "LIBERICA_NIK"
                JavaDistribution.SAP_MACHINE.name shouldBe "SAP_MACHINE"
            }

            should("return None for an unknown short code") {
                JavaDistribution.fromShortCode("dragonwell") shouldBe None
            }

            should("return None for the deliberately excluded live short codes") {
                JavaDistribution.fromShortCode("albba") shouldBe None
                JavaDistribution.fromShortCode("gln") shouldBe None
                JavaDistribution.fromShortCode("trava") shouldBe None
            }
        }

        context("JavaDistribution.parseVersionToken") {

            should("strip a recognised short-code suffix and resolve the distribution") {
                JavaDistribution.parseVersionToken("17.0.2-tem") shouldBe ("17.0.2" to Some(JavaDistribution.TEMURIN))
            }

            should("treat the last hyphen-delimited segment as the short code") {
                JavaDistribution.parseVersionToken("17.0.2.1-2-tem") shouldBe
                    ("17.0.2.1-2" to Some(JavaDistribution.TEMURIN))
            }

            should("leave the token verbatim when there is no hyphen") {
                JavaDistribution.parseVersionToken("21.0.1") shouldBe ("21.0.1" to None)
            }

            should("leave the token verbatim when the trailing segment is unknown") {
                JavaDistribution.parseVersionToken("3.0.0-rc-1") shouldBe ("3.0.0-rc-1" to None)
            }

            should("leave the token verbatim when the trailing segment is an excluded short code") {
                JavaDistribution.parseVersionToken("17.0.2-albba") shouldBe ("17.0.2-albba" to None)
            }
        }
    })
