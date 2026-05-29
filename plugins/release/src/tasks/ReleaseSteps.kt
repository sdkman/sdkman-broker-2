package io.sdkman.kotlintoolchain.plugins.release.tasks

import io.sdkman.kotlintoolchain.plugins.release.Settings
import io.sdkman.kotlintoolchain.plugins.release.checks.ReleaseChecks
import io.sdkman.kotlintoolchain.plugins.release.git.GitRepo
import io.sdkman.kotlintoolchain.plugins.release.version.SemVer
import io.sdkman.kotlintoolchain.plugins.release.version.VersionPipeline

/**
 * Internal helpers shared by [createRelease], [pushRelease], and [release].
 *
 * Top-level non-`@TaskAction` Kotlin functions are fine inside a plugin
 * module — only functions exposed as task actions need the marker.
 */

/** Run pre-release checks against [repo]; throw on any failure. */
internal fun verifyOrFail(repo: GitRepo, settings: Settings, pipeline: VersionPipeline) {
    val failures = ReleaseChecks(settings, pipeline).run(repo)
    if (failures.isNotEmpty()) {
        error(failures.joinToString(prefix = "Pre-release checks failed:\n  - ", separator = "\n  - "))
    }
}

/**
 * Determine the next release version and create an annotated tag at HEAD
 * for it. No push.
 *
 * Returns the tag short name (e.g. `v1.2.3`).
 *
 * Throws when HEAD is already on a release tag (nothing to release) — callers
 * decide whether to swallow that or surface it.
 */
internal fun createReleaseTag(
    repo: GitRepo,
    settings: Settings,
    pipeline: VersionPipeline,
): String {
    val next: SemVer = pipeline.nextReleaseVersion(repo)
        ?: error(
            "HEAD is already on a release tag. Make a new commit (or use " +
                "RELEASE_FORCE_VERSION) before running createRelease/release.",
        )
    val tagName = pipeline.tagNameFor(next)
    if (repo.findTag(tagName) != null) {
        error("Tag '$tagName' already exists. Delete it (`git tag -d $tagName`) or bump past it before retrying.")
    }
    repo.createAnnotatedTag(name = tagName, message = "Release $next")
    println("Created release tag $tagName at HEAD (${repo.headCommit().abbreviate(7).name()}).")
    return tagName
}

/** Push the named tag to `origin`. */
internal fun pushReleaseTag(repo: GitRepo, tagName: String) {
    repo.pushTag(tagName)
    println("Pushed tag $tagName to origin.")
}
