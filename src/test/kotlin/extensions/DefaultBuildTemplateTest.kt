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
import jetbrains.buildServer.configs.kotlin.Parameter
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.Template
import jetbrains.buildServer.configs.kotlin.buildSteps.GradleBuildStep
import jetbrains.buildServer.configs.kotlin.triggers.VcsTrigger
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DefaultBuildTemplateTest {

    private val vcsRoot = GitVcsRoot()
    private lateinit var template: Template

    @BeforeEach
    fun init() {
        DslContext.clearParameters()
        DslContext.addParameters(Pair("dummy.parameter", "value"))
        template = Project().defaultPluginBuildTemplate(vcsRoot)
    }

    @Test
    fun `template name`() {
        assertEquals("build plugin", template.name)
    }

    @Test
    fun `vcs root is configured`() {
        assertEquals(1, template.vcs.entries.size)
        assertSame(vcsRoot, template.vcs.entries[0].root)
    }

    @Test
    fun `gradle build step`() {
        assertEquals(1, template.steps.items.size)

        val buildStep = template.steps.items[0] as GradleBuildStep
        assertEquals("GradleBuild", buildStep.id)
        assertEquals("%gradle.tasks%", buildStep.tasks)
        assertEquals("%gradle.opts%", buildStep.gradleParams)
        assertEquals("%java.home%", buildStep.jdkHome)
    }

    @Test
    fun `vcs trigger`() {
        assertEquals(1, template.triggers.items.size)

        val vcsTrigger = template.triggers.items[0] as VcsTrigger
        assertEquals("vcsTrigger", vcsTrigger.id)
        assertEquals("", vcsTrigger.branchFilter)

        val expectedTriggerRules = """
        -:.github/**
        -:README.adoc
        """.trimIndent()
        assertEquals(expectedTriggerRules, vcsTrigger.triggerRules)
    }

    @Test
    fun `execution time out is set`() {
        assertEquals(15, template.failureConditions.executionTimeoutMin)
    }

    @Test
    fun `template parameters`() {
        val parameters = template.params.params

        assertEquals(3, parameters.size)
        assertEquals(Parameter("gradle.opts", ""), parameters[0])
        assertEquals(Parameter("gradle.tasks", "clean build"), parameters[1])
        assertEquals(Parameter("java.home", "%java8.home%"), parameters[2])
    }

    @Test
    fun `alternative java home`() {
        DslContext.addParameters(Pair("java.home", "%java17.home%"))
        template = Project().defaultPluginBuildTemplate(vcsRoot)

        val parameters = template.params.params
        assertEquals(3, parameters.size)
        assertEquals(Parameter("java.home", "%java17.home%"), parameters[2])
    }
}
