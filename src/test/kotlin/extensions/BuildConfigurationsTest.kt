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

import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.DslContext
import jetbrains.buildServer.configs.kotlin.Parameter
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.Template
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BuildConfigurationsTest {

    @BeforeEach
    fun init() {
        DslContext.clearParameters()
    }

    @Test
    fun `empty api versions`() {
        DslContext.addParameters(Pair("teamcity.api.versions", ""))

        val e = assertThrows<IllegalArgumentException> { Project().createApiBuildConfigurations(Template()) }

        assertEquals("Empty API versions list", e.message)
    }

    @Test
    fun `single api version`() {
        DslContext.addParameters(Pair("teamcity.api.versions", "2025.03"))

        val builds = Project().createApiBuildConfigurations(Template())

        assertEquals(1, builds.size)
        assertBuild("Build1", "2025.03", builds[0])
        assertEquals("build/distributions/*.zip", builds[0].artifactRules)
    }

    @Test
    fun `multiple api versions`() {
        DslContext.addParameters(Pair("teamcity.api.versions", "2018.1,2022.04,2025.03"))

        val builds = Project().createApiBuildConfigurations(Template())

        assertEquals(3, builds.size)
        assertBuild("Build1", "2018.1", builds[0])
        assertBuild("Build2", "2022.04", builds[1])
        assertBuild("Build3", "2025.03", builds[2])
    }

    @Test
    fun `strip spaces from api versions`() {
        DslContext.addParameters(Pair("teamcity.api.versions", "2018.1 , 2022.04, 2025.03 "))

        val builds = Project().createApiBuildConfigurations(Template())

        assertEquals(3, builds.size)
        assertBuild("Build1", "2018.1", builds[0])
        assertBuild("Build2", "2022.04", builds[1])
        assertBuild("Build3", "2025.03", builds[2])
    }

    private fun assertBuild(id: String, version: String, build: BuildType) {
        assertEquals(DslContext.createId(id), build.id)
        assertEquals("Build - TeamCity $version", build.name)
        val parameters = build.params.params
        assertEquals(1, parameters.size)
        assertEquals(Parameter("gradle.opts", "-Pteamcity.api.version=$version"), parameters[0])
    }

    @Test
    fun `optional tasks`() {
        DslContext.addParameters(Pair("teamcity.api.versions", "2025.03"))
        DslContext.addParameters(Pair("gradle.tasks", "clean build sonar"))

        val builds = Project().createApiBuildConfigurations(Template())

        val parameters = builds[0].params.params
        assertEquals(2, parameters.size)
        assertEquals(Parameter("gradle.tasks", "clean build sonar"), parameters[1])
    }

    @Test
    fun `additional gradle options`() {
        DslContext.addParameters(Pair("teamcity.api.versions", "2025.03"))
        DslContext.addParameters(Pair("gradle.options", "-Ptest=value"))

        val builds = Project().createApiBuildConfigurations(Template())

        val parameters = builds[0].params.params
        assertEquals(1, parameters.size)
        assertEquals(Parameter("gradle.opts", "-Pteamcity.api.version=2025.03 -Ptest=value"), parameters[0])
    }

    @Test
    fun `uses template`() {
        DslContext.addParameters(Pair("teamcity.api.versions", "2025.03"))

        val template = Template()
        val build = Project().createApiBuildConfigurations(template)[0]

        assertEquals(1, build.templates.size)
        assertSame(template, build.templates[0])
    }
}
