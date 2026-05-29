package io.sdkman.kotlintoolchain.plugins.release

import org.jetbrains.amper.plugins.Configurable

/**
 * Configuration for the release plugin.
 *
 * MVP port of axion-release-plugin's `scmVersion { ... }` DSL, scoped down to
 * what the 5 release tasks need. See README.md for the axion mapping.
 */
@Configurable
interface Settings {
    /**
     * Path to the Git repository, relative to the module root, or absolute.
     *
     * When empty, the plugin ascends from the module root looking for a `.git`
     * directory. This handles the typical "monorepo with one Git repo at the
     * project root" layout without forcing every module to spell it out.
     */
    val repoDir: String get() = ""

    /** Tag prefix used to identify release tags. Default `v`, e.g. `v1.2.3`. */
    val tagPrefix: String get() = "v"

    /**
     * Optional separator between the prefix and the version number.
     *
     * Empty by default, so a tag looks like `v1.2.3`. Set to `-` to get
     * `v-1.2.3`, mirroring axion's `tag.versionSeparator`.
     */
    val versionSeparator: String get() = ""

    /** Version returned when no release tags exist yet. */
    val initialVersion: String get() = "0.1.0"

    /**
     * Skip the uncommitted-changes pre-release check.
     *
     * Equivalent to axion's `ignoreUncommittedChanges = true`.
     */
    val ignoreUncommittedChanges: Boolean get() = false

    /**
     * Regex matched against the current branch name. Releases on
     * non-matching branches fail unless `RELEASE_DISABLE_CHECKS=true`.
     *
     * Empty disables the gate (any branch may release). Default:
     * `main|master`.
     */
    val releaseBranchPattern: String get() = "main|master"

    /** Pre-release check toggles. */
    val checks: ChecksSettings
}

/** Toggles for individual pre-release checks. */
@Configurable
interface ChecksSettings {
    /** Fail the release if the working tree has staged or unstaged changes. */
    val uncommittedChanges: Boolean get() = true

    /** Fail the release if local has unpushed commits or remote has unpulled commits. */
    val aheadOfRemote: Boolean get() = true
}
