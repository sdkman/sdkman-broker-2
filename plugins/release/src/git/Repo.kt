package io.sdkman.kotlintoolchain.plugins.release.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.BranchTrackingStatus
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.nio.file.Files
import java.nio.file.Path

/**
 * Information about a Git tag relevant to release versioning.
 *
 * @property name full tag name, e.g. `v1.2.3`
 * @property versionString the tag name with the configured prefix (and optional
 *   separator) stripped, e.g. `1.2.3`
 * @property commitId the commit the tag points at (peeled if annotated)
 */
data class TagInfo(val name: String, val versionString: String, val commitId: ObjectId)

/** Result of walking tags up from HEAD. */
sealed interface NearestTag {
    /** No release tag found anywhere in the history reachable from HEAD. */
    data object None : NearestTag

    /** A tag was found. `onHead = true` when HEAD points exactly at the tagged commit. */
    data class Found(val tag: TagInfo, val onHead: Boolean) : NearestTag
}

/** Counts of unpushed local / unpulled remote commits relative to the configured upstream. */
data class AheadBehind(val ahead: Int, val behind: Int) {
    val isInSync: Boolean get() = ahead == 0 && behind == 0
}

/**
 * Thin JGit wrapper exposing only what the release plugin needs.
 *
 * Always close (or use [use]) so the underlying repository is released.
 */
class GitRepo private constructor(private val git: Git) : AutoCloseable {
    private val repository: Repository get() = git.repository

    /**
     * Branch name. When the working tree is on a detached HEAD (typical on
     * CI checkouts), JGit returns the abbreviated commit SHA, which is rarely
     * what callers want — they should consult [isDetached] and supply an
     * override (axion's `RELEASE_OVERRIDDEN_BRANCH_NAME`).
     */
    fun branchName(): String = repository.branch ?: ""

    fun isDetached(): Boolean {
        val full = repository.fullBranch ?: return true
        return !full.startsWith(Constants.R_HEADS)
    }

    fun isDirty(): Boolean {
        val status = git.status().call()
        return status.hasUncommittedChanges() || status.untracked.isNotEmpty()
    }

    /**
     * Compute ahead/behind versus the upstream of the current branch.
     *
     * Returns `null` when the current branch has no configured upstream
     * (or when on detached HEAD) — callers decide whether that should fail
     * the release or be silently treated as "in sync".
     */
    fun aheadBehind(): AheadBehind? {
        val branch = repository.branch ?: return null
        val tracking = BranchTrackingStatus.of(repository, branch) ?: return null
        return AheadBehind(tracking.aheadCount, tracking.behindCount)
    }

    /**
     * Walk tags from HEAD using the "first tag encountered walking up" rule
     * (axion's default; not the highest-version-in-tree variant). Returns
     * [NearestTag.None] when no tags match the prefix anywhere in the history
     * reachable from HEAD.
     */
    fun nearestTagFromHead(prefix: String, separator: String): NearestTag {
        val headId = repository.resolve(Constants.HEAD) ?: return NearestTag.None
        val tagsByCommit = listMatchingTags(prefix, separator).groupBy { it.commitId }

        if (tagsByCommit.isEmpty()) return NearestTag.None

        RevWalk(repository).use { walk ->
            walk.markStart(walk.parseCommit(headId))
            for (commit in walk) {
                val tags = tagsByCommit[commit.id] ?: continue
                val best = tags.maxByOrNull { it.versionString } ?: continue
                return NearestTag.Found(best, onHead = commit.id == headId)
            }
        }
        return NearestTag.None
    }

    /**
     * List release tags whose name matches the configured prefix.
     *
     * For lightweight tags `Ref.objectId` is the commit; for annotated tags
     * we have to peel through the tag object first.
     */
    fun listMatchingTags(prefix: String, separator: String): List<TagInfo> {
        val expected = prefix + separator
        return git.tagList().call().mapNotNull { ref -> ref.toTagInfo(expected) }
    }

    private fun Ref.toTagInfo(expectedPrefix: String): TagInfo? {
        val shortName = name.removePrefix(Constants.R_TAGS)
        if (!shortName.startsWith(expectedPrefix)) return null
        val version = shortName.substring(expectedPrefix.length)
        val commitId = peeledCommitId(this) ?: return null
        return TagInfo(name = shortName, versionString = version, commitId = commitId)
    }

    private fun peeledCommitId(ref: Ref): ObjectId? {
        val peeled = repository.refDatabase.peel(ref)
        return peeled.peeledObjectId ?: peeled.objectId
    }

    fun headCommit(): RevCommit {
        val id = repository.resolve(Constants.HEAD) ?: error("Repository has no HEAD")
        return RevWalk(repository).use { it.parseCommit(id) }
    }

    /**
     * Tags whose name matches [prefix]+[separator] and that point exactly at
     * HEAD. Used by `pushRelease` to discover what `createRelease` just made.
     */
    fun matchingTagsAtHead(prefix: String, separator: String): List<TagInfo> {
        val headId = repository.resolve(Constants.HEAD) ?: return emptyList()
        return listMatchingTags(prefix, separator).filter { it.commitId == headId }
    }

    /** Create an annotated tag at HEAD. Fails if a tag of the same name already exists. */
    fun createAnnotatedTag(name: String, message: String) {
        git.tag()
            .setName(name)
            .setMessage(message)
            .setAnnotated(true)
            .call()
    }

    /** Look up an existing tag reference by short name (e.g. `v1.2.3`). */
    fun findTag(name: String): Ref? = repository.refDatabase.findRef(Constants.R_TAGS + name)

    /**
     * Push a single tag to the configured `origin`. Honors HTTPS credentials
     * supplied via env (`GIT_USERNAME`/`GIT_PASSWORD`, or `GITHUB_TOKEN`),
     * otherwise falls back to JGit defaults (SSH agent, `~/.ssh/config`).
     *
     * Always pushes only the tag itself — branches/commits are out of scope
     * (axion's `pushTagsOnly` is the default here).
     */
    fun pushTag(name: String, remote: String = Constants.DEFAULT_REMOTE_NAME) {
        val refSpec = RefSpec("${Constants.R_TAGS}$name:${Constants.R_TAGS}$name")
        val push = git.push()
            .setRemote(remote)
            .setRefSpecs(refSpec)
        credentialsFromEnv()?.let { push.setCredentialsProvider(it) }
        push.call().forEach { result ->
            result.remoteUpdates.forEach { update ->
                val ok = update.status == org.eclipse.jgit.transport.RemoteRefUpdate.Status.OK ||
                    update.status == org.eclipse.jgit.transport.RemoteRefUpdate.Status.UP_TO_DATE
                if (!ok) error("Push of $name failed: ${update.status} ${update.message ?: ""}")
            }
        }
    }

    private fun credentialsFromEnv(): UsernamePasswordCredentialsProvider? {
        val token = System.getenv("GITHUB_TOKEN")
        if (!token.isNullOrBlank()) {
            // GitHub: any non-empty username is accepted when using a token as the password.
            return UsernamePasswordCredentialsProvider("x-access-token", token)
        }
        val user = System.getenv("GIT_USERNAME")
        val pass = System.getenv("GIT_PASSWORD")
        if (!user.isNullOrBlank() && !pass.isNullOrBlank()) {
            return UsernamePasswordCredentialsProvider(user, pass)
        }
        return null
    }

    override fun close() = git.close()

    companion object {
        /**
         * Open the Git repository associated with [start]. When [start] is
         * empty/null, we ascend from [moduleRoot] looking for a `.git`
         * directory. Mirrors how a developer would invoke `git` from inside
         * any subdirectory of the repo.
         */
        fun open(moduleRoot: Path, configuredRepoDir: String): GitRepo {
            val explicit = configuredRepoDir.takeIf { it.isNotBlank() }?.let {
                val p = java.nio.file.Paths.get(it)
                if (p.isAbsolute) p else moduleRoot.resolve(p).normalize()
            }
            val anchor = explicit ?: moduleRoot
            val gitDir = ascendForGitDir(anchor)
                ?: error("No Git repository found at or above $anchor")
            val git = Git.open(gitDir.toFile())
            return GitRepo(git)
        }

        private fun ascendForGitDir(start: Path): Path? {
            var current: Path? = start.toAbsolutePath().normalize()
            while (current != null) {
                val candidate = current.resolve(".git")
                if (Files.isDirectory(candidate) || Files.isRegularFile(candidate)) return current
                current = current.parent
            }
            return null
        }
    }
}
