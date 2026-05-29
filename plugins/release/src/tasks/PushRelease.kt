package io.sdkman.kotlintoolchain.plugins.release.tasks

import io.sdkman.kotlintoolchain.plugins.release.Settings
import io.sdkman.kotlintoolchain.plugins.release.git.GitRepo
import io.sdkman.kotlintoolchain.plugins.release.version.SemVer
import io.sdkman.kotlintoolchain.plugins.release.version.VersionPipeline
import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.TaskAction
import java.nio.file.Path

/**
 * Push the release tag at HEAD to `origin`.
 *
 * Mirrors axion's `pushRelease`. When `RELEASE_FORCE_VERSION` is set, push
 * that specific tag instead of relying on HEAD being tagged. Otherwise, the
 * task expects [createRelease] (or a hand-run `git tag`) to have placed a
 * matching tag at HEAD.
 *
 * Authentication is picked up from env: `GITHUB_TOKEN`, or `GIT_USERNAME` +
 * `GIT_PASSWORD`. SSH falls back to JGit defaults (system agent, ssh config).
 */
@TaskAction
fun pushRelease(
    @Input(inferTaskDependency = false) moduleRootDir: Path,
    settings: Settings,
) {
    val pipeline = VersionPipeline(settings)
    GitRepo.open(moduleRootDir, settings.repoDir).use { repo ->
        val tagName = pickTagToPush(repo, settings, pipeline)
        pushReleaseTag(repo, tagName)
    }
}

/** Pick which tag to push, or fail with an explanation. */
internal fun pickTagToPush(
    repo: GitRepo,
    settings: Settings,
    pipeline: VersionPipeline,
    env: Map<String, String?> = System.getenv(),
): String {
    val forced = env["RELEASE_FORCE_VERSION"]?.takeIf { it.isNotBlank() }
    if (forced != null) {
        val parsed = SemVer.parseOrNull(forced)
            ?: error("RELEASE_FORCE_VERSION='$forced' is not MAJOR.MINOR.PATCH")
        val tagName = pipeline.tagNameFor(parsed)
        if (repo.findTag(tagName) == null) {
            error("RELEASE_FORCE_VERSION asks to push '$tagName' but no such tag exists locally.")
        }
        return tagName
    }
    val atHead = repo.matchingTagsAtHead(settings.tagPrefix, settings.versionSeparator)
    return when {
        atHead.isEmpty() -> error(
            "No release tag at HEAD to push. Run createRelease first, or set " +
                "RELEASE_FORCE_VERSION=X to push tag '${settings.tagPrefix}${settings.versionSeparator}X'.",
        )
        atHead.size == 1 -> atHead.single().name
        else -> {
            // Multiple matching tags at HEAD — push the highest by SemVer to be deterministic.
            atHead.mapNotNull { tag ->
                SemVer.parseOrNull(tag.versionString)?.let { sv -> sv to tag.name }
            }.maxByOrNull { it.first }?.second
                ?: error("Multiple matching tags at HEAD but none parse as SemVer: ${atHead.map { it.name }}")
        }
    }
}
