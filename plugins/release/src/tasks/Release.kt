package com.example.release.tasks

import com.example.release.Settings
import com.example.release.git.GitRepo
import com.example.release.version.VersionPipeline
import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.TaskAction
import java.nio.file.Path

/**
 * Atomic combo: pre-release checks → create the release tag locally → push it.
 *
 * Mirrors axion's `release` (the recommended day-to-day entry point). All
 * three steps run in-process with the same `GitRepo` handle, so there's no
 * window where another process could see a tag that the plugin then aborts.
 */
@TaskAction
fun release(
    @Input(inferTaskDependency = false) moduleRootDir: Path,
    settings: Settings,
) {
    val pipeline = VersionPipeline(settings)
    GitRepo.open(moduleRootDir, settings.repoDir).use { repo ->
        verifyOrFail(repo, settings, pipeline)
        val tagName = createReleaseTag(repo, settings, pipeline)
        pushReleaseTag(repo, tagName)
    }
}
