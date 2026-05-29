package io.sdkman.kotlintoolchain.plugins.release.tasks

import io.sdkman.kotlintoolchain.plugins.release.Settings
import io.sdkman.kotlintoolchain.plugins.release.git.GitRepo
import io.sdkman.kotlintoolchain.plugins.release.version.VersionPipeline
import org.jetbrains.amper.plugins.ExecutionAvoidance
import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.Output
import org.jetbrains.amper.plugins.TaskAction
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.div
import kotlin.io.path.writeText

/**
 * Write the inferred version to a `release.properties` file at the root of [outputDir], as
 * a single `release=<version>` line.
 *
 * This is a convenience companion to [writeVersion] for consumers that already read the
 * classpath resource `release.properties` with `java.util.Properties.load()` (legacy
 * sdkman-broker shape). New code should prefer `META-INF/release/version.txt` produced by
 * [writeVersion]; this task is purely an in-house compatibility shim.
 *
 * Execution avoidance is disabled because version derivation has hidden inputs (Git history)
 * that Toolchain has no way to fingerprint.
 */
@OptIn(ExperimentalPathApi::class)
@TaskAction(executionAvoidance = ExecutionAvoidance.Disabled)
fun writeReleaseProperties(
    @Input(inferTaskDependency = false) moduleRootDir: Path,
    @Output outputDir: Path,
    settings: Settings,
) {
    val pipeline = VersionPipeline(settings)
    val inferred = GitRepo.open(moduleRootDir, settings.repoDir).use { pipeline.infer(it) }

    outputDir.deleteRecursively()
    val propertiesFile = outputDir / "release.properties"
    propertiesFile.createParentDirectories()
    propertiesFile.writeText("release=${inferred.version}\n")
}
