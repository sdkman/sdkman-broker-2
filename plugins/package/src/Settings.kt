package io.sdkman.kotlintoolchain.plugins.pkg

import org.jetbrains.amper.plugins.Configurable

@Configurable
interface Settings {
    /**
     * Optional override for the file name of the staged JAR (no extension).
     *
     * Defaults to the module name. For instance, the module `sdkman-broker-2` produces
     * `build/libs/sdkman-broker-2.jar` unless overridden.
     */
    val artifactName: String?
}
