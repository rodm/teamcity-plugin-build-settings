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

import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.Template
import jetbrains.buildServer.configs.kotlin.VcsRoot
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.triggers.vcs

fun Project.defaultPluginBuildTemplate(vcsRoot: VcsRoot): Template {
    return template {
        id("Build")
        name = "build plugin"

        vcs {
            root(vcsRoot)
        }

        steps {
            gradle {
                id = "GradleBuild"
                tasks = "%gradle.tasks%"
                gradleParams = "%gradle.opts%"
                useGradleWrapper = true
                enableStacktrace = true
                jdkHome = "%java.home%"
            }
        }

        triggers {
            vcs {
                id = "vcsTrigger"
                branchFilter = ""
                triggerRules = """
                    -:.github/**
                    -:README.adoc
                """.trimIndent()
            }
        }

        failureConditions {
            executionTimeoutMin = 15
        }

        features {
            feature {
                id = "perfmon"
                type = "perfmon"
            }
        }

        params {
            param("gradle.opts", "")
            param("gradle.tasks", "clean build")
            param("java.home", "%java8.home%")
        }
    }
}
