/*
 * The release/version logic in this plugin is a Kotlin port of
 * allegro/axion-release-plugin (https://github.com/allegro/axion-release-plugin),
 * licensed under the Apache License 2.0.
 */

package io.sdkman.kotlintoolchain.plugins.release.version

import io.sdkman.kotlintoolchain.plugins.release.Settings
import io.sdkman.kotlintoolchain.plugins.release.git.GitRepo
import io.sdkman.kotlintoolchain.plugins.release.git.NearestTag

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
 *  4. **Decorate** — append the sanitized branch name (axion's
 *     `versionWithBranch`), except on `main`/`master` or a detached HEAD, where
 *     the version stays bare. Runs on every read, release or not.
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
            val isRelease = !forceSnapshot
            return InferredVersion(
                version = composeVersion(forceVersion, repo, isRelease),
                isRelease = isRelease,
                source = VersionSource.ForcedVersion,
            )
        }

        return when (val nearest = repo.nearestTagFromHead(settings.tagPrefix, settings.versionSeparator)) {
            NearestTag.None -> {
                val seed = SemVer.parseOrNull(settings.initialVersion)
                    ?: error("Invalid initialVersion '${settings.initialVersion}' — must be MAJOR.MINOR.PATCH")
                InferredVersion(
                    version = composeVersion(seed.toString(), repo, isRelease = false),
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
                InferredVersion(
                    version = composeVersion(effective.toString(), repo, isRelease = onTag),
                    isRelease = onTag,
                    source = VersionSource.Tag,
                )
            }
        }
    }

    /**
     * The next release version that [createRelease][io.sdkman.kotlintoolchain.plugins.release.tasks.createRelease]
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

    /**
     * Branch name used for decoration and the release-branch gate.
     *
     * `RELEASE_OVERRIDDEN_BRANCH_NAME` always wins. Otherwise a detached HEAD —
     * the default checkout state on CI (`actions/checkout`) — must not
     * contribute a branch name: JGit reports the abbreviated commit SHA there,
     * which would leak into the decorated version (e.g. `1.2.4-<sha>`) and
     * spuriously trip the release-branch gate. Report it as "no branch" so
     * decoration is skipped and the gate isn't matched against a SHA. Set the
     * override to opt back into branch-aware behavior on a detached checkout.
     */
    fun effectiveBranchName(repo: GitRepo): String {
        env["RELEASE_OVERRIDDEN_BRANCH_NAME"]?.takeIf { it.isNotBlank() }?.let { return it }
        if (repo.isDetached()) return ""
        return repo.branchName()
    }

    /**
     * Compose the final version string from [base], in axion's phase order:
     * decorate, then append `-SNAPSHOT` for non-release builds, then sanitize.
     *
     * [decorate] runs for releases too — it is what keeps them bare. axion's
     * `versionWithBranch` skips `main`/`master` and detached HEAD, and the
     * release workflow resolves the version while HEAD is detached, so the
     * released version (and what is stamped into `release.properties` /
     * `version.txt`) stays bare (e.g. `1.2.4`); a dev build on a named branch
     * becomes `1.2.4-<branch>-SNAPSHOT`.
     */
    private fun composeVersion(base: String, repo: GitRepo, isRelease: Boolean): String {
        val decorated = decorate(base, repo)
        return sanitize(if (isRelease) decorated else appendSnapshot(decorated))
    }

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
