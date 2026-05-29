package io.sdkman.kotlintoolchain.plugins.release.version

/**
 * Minimal `MAJOR.MINOR.PATCH` Semantic Versioning value class.
 *
 * Pre-release identifiers and build metadata aren't supported in MVP — the
 * release plugin only ever produces decorated versions like `1.2.3-foo` via
 * the [VersionPipeline], not raw SemVer pre-releases on tags themselves.
 */
data class SemVer(val major: Int, val minor: Int, val patch: Int) : Comparable<SemVer> {
    override fun toString(): String = "$major.$minor.$patch"

    fun incrementPatch(): SemVer = copy(patch = patch + 1)

    override fun compareTo(other: SemVer): Int =
        compareValuesBy(this, other, SemVer::major, SemVer::minor, SemVer::patch)

    companion object {
        private val PATTERN = Regex("""^(\d+)\.(\d+)\.(\d+)$""")

        fun parseOrNull(s: String): SemVer? {
            val m = PATTERN.matchEntire(s.trim()) ?: return null
            val (maj, min, pat) = m.destructured
            return SemVer(maj.toInt(), min.toInt(), pat.toInt())
        }

        fun parse(s: String): SemVer =
            parseOrNull(s) ?: error("'$s' is not a valid MAJOR.MINOR.PATCH version")
    }
}
