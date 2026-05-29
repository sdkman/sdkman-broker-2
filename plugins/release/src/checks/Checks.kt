package io.sdkman.kotlintoolchain.plugins.release.checks

import io.sdkman.kotlintoolchain.plugins.release.Settings
import io.sdkman.kotlintoolchain.plugins.release.git.GitRepo
import io.sdkman.kotlintoolchain.plugins.release.version.VersionPipeline

/**
 * Pre-release checks (axion's `verifyRelease`).
 *
 * MVP supports the two cheap ones: working-tree-clean and not-ahead-of-remote.
 * The snapshot-dependency check from axion is out of MVP — Toolchain doesn't
 * surface resolved dependency coordinates to plugins yet without significant
 * extra wiring.
 */
class ReleaseChecks(
    private val settings: Settings,
    private val pipeline: VersionPipeline,
    private val env: Map<String, String?> = System.getenv(),
) {
    /**
     * Run all configured checks. Returns the list of failure messages — empty
     * means everything passed. Callers convert non-empty into `error(...)`.
     */
    fun run(repo: GitRepo): List<String> {
        if (env["RELEASE_DISABLE_CHECKS"].asBoolean()) return emptyList()

        val failures = mutableListOf<String>()

        if (settings.checks.uncommittedChanges &&
            !settings.ignoreUncommittedChanges &&
            !env["RELEASE_DISABLE_UNCOMMITTED_CHECK"].asBoolean()
        ) {
            if (repo.isDirty()) {
                failures += "Working tree has uncommitted changes. " +
                    "Commit or stash them, set ignoreUncommittedChanges=true in module.yaml, " +
                    "or run with RELEASE_DISABLE_UNCOMMITTED_CHECK=true."
            }
        }

        if (settings.checks.aheadOfRemote &&
            !env["RELEASE_DISABLE_REMOTE_CHECK"].asBoolean()
        ) {
            val ab = repo.aheadBehind()
            when {
                ab == null -> {
                    if (!repo.isDetached()) {
                        failures += "Current branch '${repo.branchName()}' has no configured upstream. " +
                            "Set one (`git push -u`) or run with RELEASE_DISABLE_REMOTE_CHECK=true."
                    }
                }
                !ab.isInSync -> {
                    failures += "Branch is out of sync with its upstream " +
                        "(ahead=${ab.ahead}, behind=${ab.behind}). " +
                        "Pull/push to align, or run with RELEASE_DISABLE_REMOTE_CHECK=true."
                }
            }
        }

        val branchPattern = settings.releaseBranchPattern
        if (branchPattern.isNotBlank()) {
            val branch = pipeline.effectiveBranchName(repo)
            if (branch.isNotBlank() && !Regex(branchPattern).matches(branch)) {
                failures += "Releases are gated to branches matching '$branchPattern'; " +
                    "current branch is '$branch'. " +
                    "Override the branch with RELEASE_OVERRIDDEN_BRANCH_NAME, " +
                    "loosen releaseBranchPattern, or run with RELEASE_DISABLE_CHECKS=true."
            }
        }

        return failures
    }
}

private fun String?.asBoolean(): Boolean =
    this != null && this.equals("true", ignoreCase = true)
