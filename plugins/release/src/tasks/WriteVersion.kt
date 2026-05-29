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
 * Write the inferred version to `META-INF/release/version.txt` inside [outputDir].
 *
 * This is the canonical "publish the version" task. Downstream consumers
 * pick the file up in one of two ways:
 *
 *  - **Build-time** — another task action declares
 *    `@Input versionFile: Path` pointing at
 *    `${tasks.writeVersion.action.outputDir}/META-INF/release/version.txt`,
 *    or `@Input versionDir: Path` pointing at the whole directory. Toolchain
 *    auto-wires the dependency by matching `@Input` and `@Output` paths.
 *  - **Runtime** — `plugin.yaml` registers [outputDir] under
 *    `generated.resources`, so the file ships in the JAR classpath. Apps
 *    read it via `getResourceAsStream("/META-INF/release/version.txt")`.
 *
 * Execution avoidance is disabled because version derivation has hidden
 * inputs (Git history) that Toolchain has no way to fingerprint.
 */
@OptIn(ExperimentalPathApi::class)
@TaskAction(executionAvoidance = ExecutionAvoidance.Disabled)
fun writeVersion(
    @Input(inferTaskDependency = false) moduleRootDir: Path,
    @Output outputDir: Path,
    settings: Settings,
) {
    val pipeline = VersionPipeline(settings)
    val inferred = GitRepo.open(moduleRootDir, settings.repoDir).use { pipeline.infer(it) }

    outputDir.deleteRecursively()
    val versionFile = outputDir / "META-INF" / "release" / "version.txt"
    versionFile.createParentDirectories()
    versionFile.writeText(inferred.version + "\n")
}
