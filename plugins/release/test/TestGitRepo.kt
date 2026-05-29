package io.sdkman.kotlintoolchain.plugins.release

import io.sdkman.kotlintoolchain.plugins.release.git.GitRepo
import org.eclipse.jgit.api.Git
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.writeText

/** [Settings] with all default values; only [Settings.checks] needs a concrete value. */
fun testSettings(): Settings = object : Settings {
    override val checks: ChecksSettings = object : ChecksSettings {}
}

/**
 * A throwaway on-disk Git repository for exercising [GitRepo]-backed logic in tests.
 *
 * Each [commit] writes a file so the commit is non-empty and distinct. Commits and tags
 * use a fixed identity and are unsigned, so tests don't depend on the developer's global
 * git config.
 */
class TestGitRepo : AutoCloseable {
    val dir: Path = Files.createTempDirectory("release-plugin-test")
    private val git: Git = Git.init().setDirectory(dir.toFile()).setInitialBranch("main").call()

    fun commit(message: String): TestGitRepo {
        (dir / "f.txt").writeText(message)
        git.add().addFilepattern(".").call()
        git.commit()
            .setMessage(message)
            .setAuthor("test", "test@example.com")
            .setCommitter("test", "test@example.com")
            .setSign(false)
            .call()
        return this
    }

    fun tag(name: String): TestGitRepo {
        git.tag().setName(name).setAnnotated(true).setMessage(name).setSigned(false).call()
        return this
    }

    fun branch(name: String): TestGitRepo {
        git.checkout().setCreateBranch(true).setName(name).call()
        return this
    }

    fun detach(): TestGitRepo {
        val head = git.repository.resolve("HEAD") ?: error("repository has no HEAD")
        git.checkout().setName(head.name).call()
        return this
    }

    fun repo(): GitRepo = GitRepo.open(dir, "")

    override fun close() {
        git.close()
        dir.toFile().deleteRecursively()
    }
}
