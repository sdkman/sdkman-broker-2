package io.sdkman.broker.domain.model

import arrow.core.None
import arrow.core.Option
import arrow.core.toOption

/**
 * Resolves the `-{short-code}` suffix carried by Java URL version tokens
 * (e.g. `17.0.2-tem`) to the full distribution enum name persisted in the
 * Postgres `versions.distribution` column (e.g. `TEMURIN`).
 *
 * Per `specs/java_distribution_short_codes.md` the mapping is fixed at 16
 * entries and only consulted when `candidate == "java"`. Unknown suffixes
 * and tokens with no `-` separator pass through verbatim with `None`
 * distribution (Business Rules 2 and 4 of the parent spec).
 */
object JavaDistribution {
    val shortCodeToEnum: Map<String, String> =
        mapOf(
            "tem" to "TEMURIN",
            "amzn" to "CORRETTO",
            "zulu" to "ZULU",
            "librca" to "LIBERICA",
            "nik" to "LIBERICA_NIK",
            "oracle" to "ORACLE",
            "open" to "OPENJDK",
            "graal" to "GRAALVM",
            "graalce" to "GRAALCE",
            "mandrel" to "MANDREL",
            "ms" to "MICROSOFT",
            "sapmchn" to "SAP_MACHINE",
            "sem" to "SEMERU",
            "kona" to "KONA",
            "bisheng" to "BISHENG",
            "jbr" to "JETBRAINS"
        )

    private val enumToShortCode: Map<String, String> =
        shortCodeToEnum.entries.associate { (shortCode, enumName) -> enumName to shortCode }

    data class Resolution(
        val version: String,
        val distribution: Option<String>
    )

    fun resolve(versionToken: String): Resolution {
        val lastHyphen = versionToken.lastIndexOf('-')
        if (lastHyphen < 0) return Resolution(versionToken, None)
        val suffix = versionToken.substring(lastHyphen + 1)
        return Option.fromNullable(shortCodeToEnum[suffix]).fold(
            ifEmpty = { Resolution(versionToken, None) },
            ifSome = { enumName -> Resolution(versionToken.substring(0, lastHyphen), enumName.toOption()) }
        )
    }

    fun shortCodeFor(enumName: String): Option<String> = Option.fromNullable(enumToShortCode[enumName])
}
