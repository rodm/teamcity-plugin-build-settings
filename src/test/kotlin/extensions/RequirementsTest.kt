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

import jetbrains.buildServer.configs.kotlin.AbsoluteId
import jetbrains.buildServer.configs.kotlin.DslContext
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.Requirement
import jetbrains.buildServer.configs.kotlin.RequirementType.CONTAINS
import jetbrains.buildServer.configs.kotlin.RequirementType.EXISTS
import jetbrains.buildServer.configs.kotlin.Template
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RequirementsTest {

    @BeforeEach
    fun init() {
        DslContext.clearParameters()
        DslContext.addParameters(Pair("teamcity.api.versions", "2018.1,2022.04,2025.03"))
    }

    @Test
    fun `builds have no requirements`() {
        val builds = Project().createApiBuildConfigurations(Template())

        configureRequirements(builds)

        assertEquals(0, builds[0].requirements.items.size)
        assertEquals(0, builds[1].requirements.items.size)
        assertEquals(0, builds[2].requirements.items.size)
    }

    @Test
    fun `all builds have requirement`() {
        DslContext.addParameters(Pair("agent.requirements", "linux"))
        val builds = Project().createApiBuildConfigurations(Template())

        configureRequirements(builds)

        val expectedRequirement = Requirement(CONTAINS, "teamcity.agent.jvm.os.name", "Linux")
        assertEquals(1, builds[0].requirements.items.size)
        assertEquals(expectedRequirement, builds[0].requirements.items[0])
        assertEquals(1, builds[1].requirements.items.size)
        assertEquals(expectedRequirement, builds[1].requirements.items[0])
        assertEquals(1, builds[2].requirements.items.size)
        assertEquals(expectedRequirement, builds[2].requirements.items[0])

    }

    @Test
    fun `build has docker requirement`() {
        DslContext.addParameters(Pair("agent.requirements", "docker"))
        val builds = Project().createApiBuildConfigurations(Template())

        configureRequirements(builds)

        val expectedRequirement = Requirement(EXISTS, "docker.server.version")
        assertEquals(expectedRequirement, builds[0].requirements.items[0])
    }

    @Test
    fun `build has macos requirement`() {
        DslContext.addParameters(Pair("agent.requirements", "macos"))
        val builds = Project().createApiBuildConfigurations(Template())

        configureRequirements(builds)

        val expectedRequirement = Requirement(CONTAINS, "teamcity.agent.jvm.os.name", "Mac OS X")
        assertEquals(expectedRequirement, builds[0].requirements.items[0])
    }

    @Test
    fun `build has solaris requirement`() {
        DslContext.addParameters(Pair("agent.requirements", "solaris"))
        val builds = Project().createApiBuildConfigurations(Template())

        configureRequirements(builds)

        val expectedRequirement = Requirement(CONTAINS, "teamcity.agent.jvm.os.name", "SunOS")
        assertEquals(expectedRequirement, builds[0].requirements.items[0])
    }

    @Test
    fun `build has windows requirement`() {
        DslContext.addParameters(Pair("agent.requirements", "windows"))
        val builds = Project().createApiBuildConfigurations(Template())

        configureRequirements(builds)

        val expectedRequirement = Requirement(CONTAINS, "teamcity.agent.jvm.os.name", "Windows")
        assertEquals(expectedRequirement, builds[0].requirements.items[0])
    }

    @Test
    fun `build has multiple requirements`() {
        DslContext.addParameters(Pair("agent.requirements", "docker,linux,macos,solaris,windows"))
        val builds = Project().createApiBuildConfigurations(Template())

        configureRequirements(builds)

        assertEquals(5, builds[0].requirements.items.size)
    }

    @Test
    fun `strip spaces from multiple requirements`() {
        DslContext.addParameters(Pair("agent.requirements", "docker ,linux, macos, solaris ,windows"))
        val builds = Project().createApiBuildConfigurations(Template())

        configureRequirements(builds)

        assertEquals(5, builds[0].requirements.items.size)
    }

    @Test
    fun `throws exception for invalid requirement`() {
        DslContext.addParameters(Pair("agent.requirements", "fakerequirement"))
        val builds = Project().createApiBuildConfigurations(Template())

        val exception = assertThrows<IllegalArgumentException> { configureRequirements(builds) }

        assertEquals("Invalid requirement: fakerequirement", exception.message)

    }

    @Test
    fun `configure requirement on specified build`() {
        DslContext.projectId = AbsoluteId("TestProject")
        DslContext.addParameters(Pair("agent.requirements", "Build1=docker"))
        val builds = Project().createApiBuildConfigurations(Template())

        configureRequirements(builds)

        assertEquals(1, builds[0].requirements.items.size)
        assertEquals(0, builds[1].requirements.items.size)
        assertEquals(0, builds[2].requirements.items.size)
    }

    @Test
    fun `throws exception for invalid build id`() {
        DslContext.addParameters(Pair("agent.requirements", "Build4=linux"))
        val builds = Project().createApiBuildConfigurations(Template())

        val exception = assertThrows<IllegalArgumentException> { configureRequirements(builds) }

        assertEquals("Invalid build id: Build4", exception.message)
    }
}
