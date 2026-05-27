package io.sdkman.amper.plugins.ktlint

import org.jetbrains.amper.plugins.Classpath
import org.jetbrains.amper.plugins.Configurable
import java.nio.file.Path

@Configurable
interface Settings {
    /**
     * Optional path to an `.editorconfig` file used as a fallback for properties not defined in any `.editorconfig`
     * on the path of a source file.
     */
    val editorConfigPath: Path?

    /**
     * Extra rule-set jars to load into ktlint via `--ruleset`. Each entry is either a Maven coordinate
     * (e.g. `com.example:my-ktlint-rules:1.0.0`) or a catalog reference (e.g. `$libs.ktlint.rules`).
     */
    val rulesetClasspath: Classpath?
}
