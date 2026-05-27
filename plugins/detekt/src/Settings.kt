/*
 * Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.plugins.detekt

import org.jetbrains.amper.plugins.Classpath
import org.jetbrains.amper.plugins.Configurable
import java.nio.file.Path

@Configurable
interface Settings {
    /**
     * Optional path to a detekt.yml configuration file.
     */
    val configFile: Path?

    /**
     * When using a custom [config file][configFile], the default values are ignored unless you also set this flag.
     */
    val buildUponDefaultConfig: Boolean get() = false

    /**
     * Run detekt with type resolution enabled (passes the module's compile classpath via `--classpath`).
     *
     * Type resolution activates additional rules that depend on type information, but some of those rules
     * (notably `UnreachableCode`) are known to produce false positives. Defaults to `false` to match the
     * behaviour of the Gradle detekt plugin's default `detekt` task; set to `true` to opt in.
     */
    val useTypeResolution: Boolean get() = false

    /**
     * Extra rule-set jars to load into Detekt via `--plugins`. Each entry is either a Maven coordinate
     * (e.g. `com.example:my-detekt-rules:1.0.0`) or a catalog reference (e.g. `$libs.detekt.rules`).
     */
    val rulesetClasspath: Classpath
}
