package io.sdkman.kotlintoolchain.plugins.release.tasks

import io.sdkman.kotlintoolchain.plugins.release.Settings
import io.sdkman.kotlintoolchain.plugins.release.checks.ReleaseChecks
import io.sdkman.kotlintoolchain.plugins.release.git.GitRepo
import io.sdkman.kotlintoolchain.plugins.release.version.VersionPipeline
import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.TaskAction
import java.nio.file.Path

/**
 * Run all configured pre-release checks and fail the task on any violation.
 *
 * Mirrors axion's `verifyRelease`. `release` and `createRelease` invoke the
 * same checks in-process before doing their side effects.
 */
@TaskAction
fun verifyRelease(
    @Input(inferTaskDependency = false) moduleRootDir: Path,
    settings: Settings,
) {
    val pipeline = VersionPipeline(settings)
    val checks = ReleaseChecks(settings, pipeline)
    GitRepo.open(moduleRootDir, settings.repoDir).use { repo ->
        val failures = checks.run(repo)
        if (failures.isNotEmpty()) {
            error(failures.joinToString(prefix = "Pre-release checks failed:\n  - ", separator = "\n  - "))
        }
        println("Pre-release checks passed.")
    }
}
