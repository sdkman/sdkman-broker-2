package io.sdkman.broker.domain.model

import arrow.core.None
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.toOption

enum class JavaDistribution(
    val shortCode: String
) {
    TEMURIN("tem"),
    CORRETTO("amzn"),
    ZULU("zulu"),
    LIBERICA("librca"),
    LIBERICA_NIK("nik"),
    ORACLE("oracle"),
    OPENJDK("open"),
    GRAALVM("graal"),
    GRAALCE("graalce"),
    MANDREL("mandrel"),
    MICROSOFT("ms"),
    SAP_MACHINE("sapmchn"),
    SEMERU("sem"),
    KONA("kona"),
    BISHENG("bisheng"),
    JETBRAINS("jbr");

    companion object {
        private val shortCodeToDistribution: Map<String, JavaDistribution> =
            entries.associateBy { it.shortCode }

        fun fromShortCode(shortCode: String): Option<JavaDistribution> = shortCodeToDistribution[shortCode].toOption()

        fun parseVersionToken(token: String): Pair<String, Option<JavaDistribution>> {
            val separatorIndex = token.lastIndexOf('-')
            if (separatorIndex < 0) return token to None
            val distribution = fromShortCode(token.substring(separatorIndex + 1))
            val version = distribution.map { token.substring(0, separatorIndex) }.getOrElse { token }
            return version to distribution
        }
    }
}
