package com.example.release.version

import com.example.release.Settings
import com.example.release.git.GitRepo
import com.example.release.git.NearestTag

/**
 * Where the version number originated, useful for log lines and diagnostics.
 */
enum class VersionSource {
    /** Read from a Git tag, possibly bumped via `incrementPatch`. */
    Tag,

    /** No tags found; settings' `initialVersion` was used as the seed. */
    Initial,

    /** `RELEASE_FORCE_VERSION` short-circuited the pipeline. */
    ForcedVersion,
}

/** Result of running the [VersionPipeline]. */
data class InferredVersion(
    /** Final decorated, possibly snapshot-suffixed, sanitized version string. */
    val version: String,

    /** True when the version corresponds to a release (HEAD on a tag, no SNAPSHOT). */
    val isRelease: Boolean,

    /** Where the base version came from. */
    val source: VersionSource,
)

/**
 * Implements axion's 6-phase pipeline (simplified to the MVP slice).
 *
 *  1. **Read** — first-tag-walking-up from HEAD ([GitRepo.nearestTagFromHead]).
 *  2. **Parse** — strip the `tagPrefix`+`versionSeparator`, parse as [SemVer].
 *  3. **Increment** — when HEAD isn't exactly on the tag, `incrementPatch`.
 *  4. **Decorate** — append the sanitized branch name on non-default branches
 *     (axion's `versionWithBranch`).
 *  5. **Append snapshot** — `-SNAPSHOT` when HEAD isn't exactly on the tag.
 *  6. **Sanitize** — replace any character outside `[A-Za-z0-9._-]` with `-`.
 *
 * `RELEASE_FORCE_VERSION` short-circuits steps 1–3; `RELEASE_FORCE_SNAPSHOT`
 * forces a snapshot suffix even on a tagged commit.
 */
class VersionPipeline(
    private val settings: Settings,
    private val env: Map<String, String?> = System.getenv(),
) {
    /** Compute the displayed version string for [repo]. */
    fun infer(repo: GitRepo): InferredVersion {
        val forceVersion = env["RELEASE_FORCE_VERSION"]?.takeIf { it.isNotBlank() }
        val forceSnapshot = env["RELEASE_FORCE_SNAPSHOT"].asBoolean()

        if (forceVersion != null) {
            val decorated = decorate(forceVersion, repo)
            val final = if (forceSnapshot) appendSnapshot(decorated) else decorated
            return InferredVersion(
                version = sanitize(final),
                isRelease = !forceSnapshot,
                source = VersionSource.ForcedVersion,
            )
        }

        return when (val nearest = repo.nearestTagFromHead(settings.tagPrefix, settings.versionSeparator)) {
            NearestTag.None -> {
                val seed = SemVer.parseOrNull(settings.initialVersion)
                    ?: error("Invalid initialVersion '${settings.initialVersion}' — must be MAJOR.MINOR.PATCH")
                val decorated = decorate(seed.toString(), repo)
                val withSnapshot = appendSnapshot(decorated)
                InferredVersion(
                    version = sanitize(withSnapshot),
                    isRelease = false,
                    source = VersionSource.Initial,
                )
            }

            is NearestTag.Found -> {
                val parsed = SemVer.parseOrNull(nearest.tag.versionString)
                    ?: error(
                        "Tag '${nearest.tag.name}' couldn't be parsed as SemVer after stripping " +
                            "prefix '${settings.tagPrefix}${settings.versionSeparator}'",
                    )
                val onTag = nearest.onHead && !forceSnapshot
                val effective = if (onTag) parsed else parsed.incrementPatch()
                val decorated = decorate(effective.toString(), repo)
                val withSnapshot = if (onTag) decorated else appendSnapshot(decorated)
                InferredVersion(
                    version = sanitize(withSnapshot),
                    isRelease = onTag,
                    source = VersionSource.Tag,
                )
            }
        }
    }

    /**
     * The next release version that [createRelease][com.example.release.tasks.createRelease]
     * would tag at HEAD: bare SemVer, no decoration, no `-SNAPSHOT`.
     *
     * Returns `null` when HEAD is already exactly on a release tag — in that
     * case there's nothing to release.
     */
    fun nextReleaseVersion(repo: GitRepo): SemVer? {
        val forceVersion = env["RELEASE_FORCE_VERSION"]?.takeIf { it.isNotBlank() }
        if (forceVersion != null) {
            return SemVer.parseOrNull(forceVersion)
                ?: error("RELEASE_FORCE_VERSION='$forceVersion' is not a valid MAJOR.MINOR.PATCH")
        }
        return when (val nearest = repo.nearestTagFromHead(settings.tagPrefix, settings.versionSeparator)) {
            NearestTag.None -> SemVer.parseOrNull(settings.initialVersion)
                ?: error("Invalid initialVersion '${settings.initialVersion}' — must be MAJOR.MINOR.PATCH")

            is NearestTag.Found -> {
                if (nearest.onHead) null
                else (SemVer.parseOrNull(nearest.tag.versionString)
                    ?: error("Tag '${nearest.tag.name}' isn't a valid SemVer")).incrementPatch()
            }
        }
    }

    /** Compose the full Git tag name for [version], honoring prefix and separator. */
    fun tagNameFor(version: SemVer): String =
        settings.tagPrefix + settings.versionSeparator + version.toString()

    /** Branch name with `RELEASE_OVERRIDDEN_BRANCH_NAME` taking precedence. */
    fun effectiveBranchName(repo: GitRepo): String =
        env["RELEASE_OVERRIDDEN_BRANCH_NAME"]?.takeIf { it.isNotBlank() }
            ?: repo.branchName()

    private fun decorate(version: String, repo: GitRepo): String {
        val branch = effectiveBranchName(repo)
        if (branch.isBlank()) return version
        // axion's "versionWithBranch": main/master are excluded; everything else
        // gets the sanitized branch suffix.
        if (branch == "main" || branch == "master") return version
        return "$version-${sanitize(branch)}"
    }

    private fun appendSnapshot(s: String): String = "$s-SNAPSHOT"

    private fun sanitize(s: String): String = s.replace(SANITIZE_PATTERN, "-")

    private companion object {
        val SANITIZE_PATTERN = Regex("[^A-Za-z0-9._-]")
    }
}

private fun String?.asBoolean(): Boolean =
    this != null && this.equals("true", ignoreCase = true)
