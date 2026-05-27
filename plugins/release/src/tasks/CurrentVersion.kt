package com.example.release.tasks

import com.example.release.Settings
import com.example.release.git.GitRepo
import com.example.release.version.VersionPipeline
import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.TaskAction
import java.nio.file.Path

/**
 * Print the version that the release plugin would resolve right now.
 *
 * Mirrors axion's `currentVersion` (alias `cV`) task.
 *
 * No `@Output`s declared — the task always re-runs, which is what we want
 * because version derivation depends on Git state Toolchain can't checksum.
 */
@TaskAction
fun currentVersion(
    @Input(inferTaskDependency = false) moduleRootDir: Path,
    settings: Settings,
) {
    val pipeline = VersionPipeline(settings)
    GitRepo.open(moduleRootDir, settings.repoDir).use { repo ->
        val inferred = pipeline.infer(repo)
        println(inferred.version)
    }
}
