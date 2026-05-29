package io.sdkman.kotlintoolchain.plugins.ktlint

import org.jetbrains.amper.plugins.Classpath
import org.jetbrains.amper.plugins.Configurable
import org.jetbrains.amper.plugins.ModuleSources

@Configurable
interface CommonKtlintSettings {
    val settings: Settings
    val sources: ModuleSources
    val ktlintClasspath: Classpath
    val rulesetClasspath: Classpath?
}
