package com.example.release.tasks

import com.example.release.Settings
import com.example.release.git.GitRepo
import com.example.release.version.VersionPipeline
import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.TaskAction
import java.nio.file.Path

/**
 * Run pre-release hooks (checks) and create the release tag locally.
 *
 * Mirrors axion's `createRelease`: never pushes. Use [pushRelease] or the
 * combined [release] task to also push.
 */
@TaskAction
fun createRelease(
    @Input(inferTaskDependency = false) moduleRootDir: Path,
    settings: Settings,
) {
    val pipeline = VersionPipeline(settings)
    GitRepo.open(moduleRootDir, settings.repoDir).use { repo ->
        verifyOrFail(repo, settings, pipeline)
        createReleaseTag(repo, settings, pipeline)
    }
}
