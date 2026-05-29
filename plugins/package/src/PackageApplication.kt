package io.sdkman.kotlintoolchain.plugins.pkg

import org.jetbrains.amper.plugins.CompilationArtifact
import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.Output
import org.jetbrains.amper.plugins.TaskAction
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createParentDirectories

/**
 * Copy the module's compiled application JAR to `build/libs/<artifactName>.jar` at the module root.
 *
 * This mirrors the shape of Gradle's `application` plugin output so that CI artifact-upload
 * workflows can publish the broker JAR from a stable, build-tool-agnostic location instead of
 * the Toolchain's internal `build/tasks/_<module>_jarJvm/<module>-jvm.jar` path.
 *
 * The `@Input jar: CompilationArtifact` parameter wires this task to the module's `jarJvm`
 * output automatically; the `@Output outputJar: Path` is project-relative on purpose (same
 * pattern as the binary-compatibility-validator plugin's `apiDump` task), so the staged file
 * lives in `build/libs/` rather than under the Toolchain's per-task scratch directory.
 */
@TaskAction
fun packageApplication(
    @Input jar: CompilationArtifact,
    @Output outputJar: Path,
) {
    jar.artifact.copyTo(outputJar.createParentDirectories(), overwrite = true)
}
