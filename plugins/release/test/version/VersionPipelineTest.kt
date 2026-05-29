package io.sdkman.kotlintoolchain.plugins.release.version

import io.sdkman.kotlintoolchain.plugins.release.TestGitRepo
import io.sdkman.kotlintoolchain.plugins.release.testSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VersionPipelineTest {
    private fun pipeline(env: Map<String, String?> = emptyMap()) = VersionPipeline(testSettings(), env)

    @Test
    fun headOnTagIsABareRelease() {
        TestGitRepo().use { r ->
            r.commit("init").tag("v1.2.3")
            val inferred = pipeline().infer(r.repo())
            assertEquals("1.2.3", inferred.version)
            assertTrue(inferred.isRelease)
        }
    }

    @Test
    fun detachedHeadOnTagStaysBare() {
        // The release workflow resolves the version on a detached HEAD; it must not
        // decorate the released version with the commit SHA.
        TestGitRepo().use { r ->
            r.commit("init").tag("v1.2.3").detach()
            val inferred = pipeline().infer(r.repo())
            assertEquals("1.2.3", inferred.version)
            assertTrue(inferred.isRelease)
        }
    }

    @Test
    fun featureBranchSnapshotIsDecoratedAndSanitized() {
        TestGitRepo().use { r ->
            r.commit("init").tag("v1.2.3").branch("feature/x").commit("work")
            val inferred = pipeline().infer(r.repo())
            assertEquals("1.2.4-feature-x-SNAPSHOT", inferred.version)
            assertFalse(inferred.isRelease)
        }
    }

    @Test
    fun mainSnapshotIsBareSnapshot() {
        TestGitRepo().use { r ->
            r.commit("init").tag("v1.2.3").commit("work")
            val inferred = pipeline().infer(r.repo())
            assertEquals("1.2.4-SNAPSHOT", inferred.version)
            assertFalse(inferred.isRelease)
        }
    }

    @Test
    fun noTagsUsesInitialVersionAsSnapshot() {
        TestGitRepo().use { r ->
            r.commit("init")
            val inferred = pipeline().infer(r.repo())
            assertEquals("0.1.0-SNAPSHOT", inferred.version)
            assertFalse(inferred.isRelease)
        }
    }

    @Test
    fun forceVersionShortCircuitsToBareRelease() {
        TestGitRepo().use { r ->
            r.commit("init").tag("v1.2.3").commit("work")
            val inferred = pipeline(mapOf("RELEASE_FORCE_VERSION" to "9.9.9")).infer(r.repo())
            assertEquals("9.9.9", inferred.version)
            assertTrue(inferred.isRelease)
        }
    }

    @Test
    fun overriddenBranchNameDecoratesEvenOnDetachedHead() {
        TestGitRepo().use { r ->
            r.commit("init").tag("v1.2.3").commit("work").detach()
            val inferred = pipeline(mapOf("RELEASE_OVERRIDDEN_BRANCH_NAME" to "release/9")).infer(r.repo())
            assertEquals("1.2.4-release-9-SNAPSHOT", inferred.version)
            assertFalse(inferred.isRelease)
        }
    }
}
