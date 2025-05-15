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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReportConfigurationTest {

    @BeforeEach
    fun init() {
        DslContext.clearParameters()
        DslContext.addParameters(Pair("dummy.options", "value"))
    }

    @Test
    fun `report configuration`() {
        val build = Project().createReportBuildConfiguration(Template())

        assertEquals(DslContext.createId("ReportCodeQuality"), build.id)
        assertEquals("Report - Code Quality", build.name)
        val parameters = build.params.params
        assertEquals(2, parameters.size)
        assertEquals(Parameter("gradle.opts", "%report.opts%"), parameters[0])
        assertEquals(Parameter("gradle.tasks", "clean build sonar"), parameters[1])
    }

    @Test
    fun `alternative report task configuration`() {
        DslContext.addParameters(Pair("report.task", "checkstyle"))
        val build = Project().createReportBuildConfiguration(Template())

        val parameters = build.params.params
        assertEquals(Parameter("gradle.tasks", "clean build checkstyle"), parameters[1])
    }

    @Test
    fun `additional gradle options`() {
        DslContext.addParameters(Pair("gradle.options", "-Ptest=value"))

        val build = Project().createReportBuildConfiguration(Template())

        val parameters = build.params.params
        assertEquals(Parameter("gradle.opts", "%report.opts% -Ptest=value"), parameters[0])
    }

    @Test
    fun `uses template`() {
        val template = Template()
        val build = Project().createReportBuildConfiguration(template)

        assertEquals(1, build.templates.size)
        assertSame(template, build.templates[0])
    }

    @Test
    fun `optional tasks are ignored`() {
        DslContext.addParameters(Pair("gradle.tasks", "jar check"))

        val build = Project().createReportBuildConfiguration(Template())

        val parameters = build.params.params
        assertEquals(Parameter("gradle.tasks", "clean build sonar"), parameters[1])
    }
}
