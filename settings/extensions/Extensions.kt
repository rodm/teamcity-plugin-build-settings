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
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.RelativeId
import jetbrains.buildServer.configs.kotlin.Requirements
import jetbrains.buildServer.configs.kotlin.Template
import jetbrains.buildServer.configs.kotlin.VcsRoot
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.toId
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

private const val GRADLE_OPTS = "gradle.opts"
private const val GRADLE_TASKS = "gradle.tasks"
private const val JAVA_HOME = "java.home"
private const val TEAMCITY_AGENT_JVM_OS_NAME = "teamcity.agent.jvm.os.name"

fun Project.createVcsRoot(): GitVcsRoot {
    val vcsName = DslContext.getParameter("vcs.name")
    val vcsUrl = DslContext.getParameter("vcs.url")
    val vcsBranch = DslContext.getParameter("vcs.branch", "master")
    val vcsRoot = GitVcsRoot {
        id(vcsName.toId())
        name = vcsName
        url = vcsUrl
        branch = "refs/heads/$vcsBranch"
        branchSpec = """
            +:refs/heads/($vcsBranch)
            +:refs/tags/(*)
        """.trimIndent()
        useTagsAsBranches = true
        checkoutPolicy = GitVcsRoot.AgentCheckoutPolicy.NO_MIRRORS
        configureAuthentication()
    }
    vcsRoot(vcsRoot)
    return vcsRoot
}

fun GitVcsRoot.configureAuthentication() {
    val vcsAuthMethod = DslContext.getParameter("vcs.auth.method", "anonymous")
    when (vcsAuthMethod.trim()) {
        "anonymous" -> configureAnonymous()
        "uploadedkey" -> configureUploadedKey()
        "password" -> configurePassword()
        else -> throw IllegalArgumentException("Invalid authentication method: $vcsAuthMethod")
    }
}

private fun GitVcsRoot.configureAnonymous() {
    authMethod = anonymous()
}

private fun GitVcsRoot.configureUploadedKey() {
    val vcsAuthUserName = DslContext.getParameter("vcs.auth.username", "")
    val vcsAuthUploadedKey = DslContext.getParameter("vcs.auth.uploadedkey", "")
    val vcsAuthPassphrase = DslContext.getParameter("vcs.auth.passphrase", "")
    authMethod = uploadedKey {
        if (vcsAuthUserName.isNotBlank()) userName = vcsAuthUserName
        if (vcsAuthUploadedKey.isNotBlank()) uploadedKey = vcsAuthUploadedKey
        if (vcsAuthPassphrase.isNotBlank()) passphrase = vcsAuthPassphrase
    }
}

private fun GitVcsRoot.configurePassword() {
    val vcsAuthUserName = DslContext.getParameter("vcs.auth.username", "")
    val vcsAuthPassword = DslContext.getParameter("vcs.auth.password", "")
    authMethod = password {
        if (vcsAuthUserName.isNotBlank()) userName = vcsAuthUserName
        if (vcsAuthPassword.isNotBlank()) password = vcsAuthPassword
    }
}

fun Project.defaultPluginBuildTemplate(vcsRoot: VcsRoot): Template {
    val javaHome = DslContext.getParameter("java.home", "%java8.home%")
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
            param(GRADLE_OPTS, "")
            param(GRADLE_TASKS, "clean build")
            param(JAVA_HOME, javaHome)
        }
    }
}

fun Project.createApiBuildConfigurations(buildTemplate: Template): MutableList<BuildType> {
    val apiVersions = DslContext.getParameter("teamcity.api.versions")
    if (apiVersions.isBlank()) throw IllegalArgumentException("Empty API versions list")

    val gradleTasks = DslContext.getParameter(GRADLE_TASKS, "")
    val gradleOptions = DslContext.getParameter("gradle.options", "")
    val builds = mutableListOf<BuildType>()
    apiVersions.split(",").forEachIndexed { index, version ->
        val build = buildType {
            templates(buildTemplate)
            id("Build${index + 1}")
            name = "Build - TeamCity ${version.trim()}"

            if (index == 0) {
                artifactRules = DslContext.getParameter("artifact.paths", "build/distributions/*.zip")
            }
            params {
                param(GRADLE_OPTS,"-Pteamcity.api.version=${version.trim()} ${gradleOptions}".trim())
                if (gradleTasks.isNotEmpty()) {
                    param(GRADLE_TASKS, gradleTasks)
                }
            }
        }
        builds.add(build)
    }
    return builds
}

fun Project.createReportBuildConfiguration(buildTemplate: Template): BuildType {
    val gradleOptions = DslContext.getParameter("gradle.options", "")
    val reportTask = DslContext.getParameter("report.task", "sonar")
    return buildType {
        templates(buildTemplate)
        id("ReportCodeQuality")
        name = "Report - Code Quality"

        params {
            param(GRADLE_OPTS, "%report.opts% ${gradleOptions}".trim())
            param(GRADLE_TASKS, "clean build ${reportTask}")
        }
    }
}

fun configureRequirements(builds: MutableList<BuildType>) {
    val buildIds = builds.map { build -> build.id }
    val requirements = DslContext.getParameter("agent.requirements", "")
    if (requirements.isNotBlank()) {
        requirements.split(",").forEach { requirement ->
            val parts = requirement.split("=")
            val name = parts.last().trim()
            val buildId = if (parts.size > 1) parts.first().trim() else ""

            if (buildId.isNotEmpty().and(RelativeId(buildId) !in buildIds))
                throw IllegalArgumentException("Invalid build id: $buildId")

            builds.filter { build -> buildId.isEmpty().or(RelativeId(buildId) == build.id) }
                .forEach { build -> applyRequirement(name, build) }
        }
    }
}

private fun applyRequirement(name: String, build: BuildType) {
    when (name) {
        "linux" -> build.requirements { linux() }
        "macos" -> build.requirements { macos() }
        "solaris" -> build.requirements { solaris() }
        "windows" -> build.requirements { windows() }
        "docker" -> build.requirements { docker() }
        else -> throw IllegalArgumentException("Invalid requirement: $name")
    }
}

private fun Requirements.linux() {
    contains(TEAMCITY_AGENT_JVM_OS_NAME, "Linux")
}

private fun Requirements.macos() {
    contains(TEAMCITY_AGENT_JVM_OS_NAME, "Mac OS X")
}

private fun Requirements.solaris() {
    contains(TEAMCITY_AGENT_JVM_OS_NAME, "SunOS")
}

private fun Requirements.windows() {
    contains(TEAMCITY_AGENT_JVM_OS_NAME, "Windows")
}

private fun Requirements.docker() {
    exists("docker.server.version")
}
