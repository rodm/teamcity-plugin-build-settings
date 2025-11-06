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
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot.AuthMethod.Anonymous
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot.AuthMethod.Password
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot.AuthMethod.UploadedKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class VcsRootTest {

    @BeforeEach
    fun init() {
        DslContext.clearParameters()
        DslContext.addParameters(Pair("dummy.parameter", "value"))
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
    fun `branch specification with additional branches`() {
        val branches = "branch-1,branch-2"
        DslContext.addParameters(Pair("vcs.branches", branches))

        val vcsRoot = Project().createVcsRoot()

        val expectedBranchSpec = """
            +:refs/heads/(master)
            +:refs/heads/(branch-1)
            +:refs/heads/(branch-2)
            +:refs/tags/(*)
        """.trimIndent()
        assertEquals(expectedBranchSpec, vcsRoot.branchSpec)
    }

    @Test
    fun `strip spaces and blank additional branches `() {
        val branches = "branch-1 , , branch-2,"
        DslContext.addParameters(Pair("vcs.branches", branches))

        val vcsRoot = Project().createVcsRoot()

        val expectedBranchSpec = """
            +:refs/heads/(master)
            +:refs/heads/(branch-1)
            +:refs/heads/(branch-2)
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

    @Test
    fun `vcs root uses anonymous authentication by default`() {
        val vcsRoot = Project().createVcsRoot()

        assertInstanceOf<Anonymous>(vcsRoot.authMethod)
    }

    @Test
    fun `vcs root uses anonymous authentication`() {
        DslContext.addParameters(Pair("vcs.auth.method", "anonymous"))

        val vcsRoot = Project().createVcsRoot()

        assertInstanceOf<Anonymous>(vcsRoot.authMethod)
    }

    @Test
    fun `vcs root uses uploaded key authentication without configuration`() {
        DslContext.addParameters(Pair("vcs.auth.method", "uploadedkey"))

        val vcsRoot = Project().createVcsRoot()

        assertInstanceOf<UploadedKey>(vcsRoot.authMethod)
        val authMethod = vcsRoot.authMethod as UploadedKey
        assertNull(authMethod.userName)
        assertNull(authMethod.uploadedKey)
        assertNull(authMethod.passphrase)
    }

    @Test
    fun `vcs root uses uploaded key authentication with configuration`() {
        DslContext.addParameters(Pair("vcs.auth.method", "uploadedkey"))
        DslContext.addParameters(Pair("vcs.auth.username", "username"))
        DslContext.addParameters(Pair("vcs.auth.uploadedkey", "uploadedkey"))
        DslContext.addParameters(Pair("vcs.auth.passphrase", "passphrase"))

        val vcsRoot = Project().createVcsRoot()

        assertInstanceOf<UploadedKey>(vcsRoot.authMethod)
        val authMethod = vcsRoot.authMethod as UploadedKey
        assertEquals("username", authMethod.userName)
        assertEquals("uploadedkey", authMethod.uploadedKey)
        assertEquals("passphrase", authMethod.passphrase)
    }

    @Test
    fun `vcs root uses password authentication without configuration`() {
        DslContext.addParameters(Pair("vcs.auth.method", "password"))

        val vcsRoot = Project().createVcsRoot()

        assertInstanceOf<Password>(vcsRoot.authMethod)
        val authMethod = vcsRoot.authMethod as Password
        assertNull(authMethod.userName)
        assertNull(authMethod.password)
    }

    @Test
    fun `vcs root uses password authentication with configuration`() {
        DslContext.addParameters(Pair("vcs.auth.method", "password"))
        DslContext.addParameters(Pair("vcs.auth.username", "username"))
        DslContext.addParameters(Pair("vcs.auth.password", "password"))

        val vcsRoot = Project().createVcsRoot()

        assertInstanceOf<Password>(vcsRoot.authMethod)
        val authMethod = vcsRoot.authMethod as Password
        assertEquals("username", authMethod.userName)
        assertEquals("password", authMethod.password)
    }

    @Test
    fun `invalid authentication method throws exception`() {
        DslContext.addParameters(Pair("vcs.auth.method", "invalid"))

        val exception = assertThrows<IllegalArgumentException> { Project().createVcsRoot() }

        assertEquals("Invalid authentication method: invalid", exception.message)
    }

    @ParameterizedTest
    @ValueSource(strings = [" anonymous", "anonymous ", " anonymous "])
    fun `authentication method names have extra spaces removed`(method: String) {
        DslContext.addParameters(Pair("vcs.auth.method", method))

        val vcsRoot = Project().createVcsRoot()

        assertInstanceOf<Anonymous>(vcsRoot.authMethod)
    }
}
