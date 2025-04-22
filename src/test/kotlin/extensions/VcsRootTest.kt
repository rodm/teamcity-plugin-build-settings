/*
 * Copyright 2025 Rod MacKenzie.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package extensions

import jetbrains.buildServer.configs.kotlin.DslContext
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot.AgentCheckoutPolicy.NO_MIRRORS
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VcsRootTest {

    @BeforeEach
    fun init() {
        DslContext.clearParameters()
    }

    @Test
    fun `vcs root id`() {
        DslContext.addParameters(Pair("vcs.name", "test-git-repo"))

        val vcsRoot = Project().createVcsRoot()

        assertEquals(DslContext.createId("TestGitRepo"), vcsRoot.id)
    }

    @Test
    fun `vcs root name`() {
        DslContext.addParameters(Pair("vcs.name", "test-git-repo"))

        val vcsRoot = Project().createVcsRoot()

        assertEquals("test-git-repo", vcsRoot.name)
    }

    @Test
    fun `vcs url`() {
        DslContext.addParameters(Pair("vcs.url", "https://github.com/rodm/test-repo"))

        val vcsRoot = Project().createVcsRoot()

        assertEquals("https://github.com/rodm/test-repo", vcsRoot.url)
    }

    @Test
    fun `default vcs branch`() {
        val vcsRoot = Project().createVcsRoot()

        assertEquals("refs/heads/master", vcsRoot.branch)
    }

    @Test
    fun `alternative vcs branch`() {
        DslContext.addParameters(Pair("vcs.branch", "main"))

        val vcsRoot = Project().createVcsRoot()

        assertEquals("refs/heads/main", vcsRoot.branch)
    }

    @Test
    fun `branch specification`() {
        val branch = "branch-name"
        DslContext.addParameters(Pair("vcs.branch", branch))

        val vcsRoot = Project().createVcsRoot()

        val expectedBranchSpec = """
            +:refs/heads/($branch)
            +:refs/tags/(*)
        """.trimIndent()
        assertEquals(expectedBranchSpec, vcsRoot.branchSpec)
    }

    @Test
    fun `use tags as branches enabled`() {
        val vcsRoot = Project().createVcsRoot()

        assertEquals(true, vcsRoot.useTagsAsBranches)
    }

    @Test
    fun `checkout policy`() {
        val vcsRoot = Project().createVcsRoot()

        assertEquals(NO_MIRRORS, vcsRoot.checkoutPolicy)
    }
}
