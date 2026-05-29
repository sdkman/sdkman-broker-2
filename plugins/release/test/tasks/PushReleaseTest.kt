package io.sdkman.kotlintoolchain.plugins.release.tasks

import io.sdkman.kotlintoolchain.plugins.release.TestGitRepo
import io.sdkman.kotlintoolchain.plugins.release.testSettings
import io.sdkman.kotlintoolchain.plugins.release.version.VersionPipeline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PushReleaseTest {
    private fun pipeline(env: Map<String, String?> = emptyMap()) = VersionPipeline(testSettings(), env)

    @Test
    fun returnsTheSingleTagAtHead() {
        TestGitRepo().use { r ->
            r.commit("init").tag("v1.0.0")
            assertEquals("v1.0.0", pickTagToPush(r.repo(), testSettings(), pipeline(), emptyMap()))
        }
    }

    @Test
    fun picksHighestSemVerWhenMultipleTagsAtHead() {
        TestGitRepo().use { r ->
            r.commit("init").tag("v1.0.0").tag("v1.2.0").tag("v1.1.0")
            assertEquals("v1.2.0", pickTagToPush(r.repo(), testSettings(), pipeline(), emptyMap()))
        }
    }

    @Test
    fun failsWhenNoTagAtHead() {
        TestGitRepo().use { r ->
            r.commit("init")
            assertFailsWith<IllegalStateException> {
                pickTagToPush(r.repo(), testSettings(), pipeline(), emptyMap())
            }
        }
    }

    @Test
    fun forceVersionEnvSelectsThatTagEvenWhenHeadIsElsewhere() {
        TestGitRepo().use { r ->
            r.commit("init").tag("v3.0.0").commit("work")
            val env = mapOf<String, String?>("RELEASE_FORCE_VERSION" to "3.0.0")
            assertEquals("v3.0.0", pickTagToPush(r.repo(), testSettings(), pipeline(env), env))
        }
    }
}
