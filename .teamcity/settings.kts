
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.DslContext
import jetbrains.buildServer.configs.kotlin.RelativeId
import jetbrains.buildServer.configs.kotlin.Requirements
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.project
import jetbrains.buildServer.configs.kotlin.toId
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot.AgentCheckoutPolicy.NO_MIRRORS
import jetbrains.buildServer.configs.kotlin.version

version = "2024.03"

project {

    val vcsName = DslContext.getParameter("vcs.name")
    val vcsUrl = DslContext.getParameter("vcs.url")
    val vcsBranch = DslContext.getParameter("vcs.branch", "master")
    val vcsRoot = GitVcsRoot {
        id(vcsName.toId())
        name = vcsName
        url = vcsUrl
        branch = "refs/heads/$vcsBranch"
        checkoutPolicy = NO_MIRRORS
    }
    vcsRoot(vcsRoot)

    params {
        param("teamcity.ui.settings.readOnly", "true")
    }

    val buildTemplate = template {
        id("Build")
        name = "build plugin"

        vcs {
            root(vcsRoot)
        }

        steps {
            gradle {
                id = "RUNNER_19"
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

    val apiVersions = DslContext.getParameter("teamcity.api.versions")
    val builds = mutableListOf<BuildType>()
    apiVersions.split(",").forEachIndexed { index, version ->
        val build = buildType {
            templates(buildTemplate)
            id("Build${index + 1}")
            name = "Build - TeamCity ${version}"

            if (index == 0) {
                artifactRules = "build/distributions/*.zip"
            } else {
                params {
                    param("gradle.opts", "-Pteamcity.api.version=${version}")
                }
            }
        }
        builds.add(build)
    }

    val reportCodeQuality = buildType {
        templates(buildTemplate)
        id("ReportCodeQuality")
        name = "Report - Code Quality"

        params {
            param("gradle.opts", "%sonar.opts%")
            param("gradle.tasks", "clean build sonarqube")
        }
    }
    builds.add(reportCodeQuality)

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

    buildTypesOrder = builds.toList()
}

fun applyRequirement(name: String, build: BuildType) {
    when (name) {
        "linux" -> build.requirements { linux() }
        "macos" -> build.requirements { macos() }
        "windows" -> build.requirements { windows() }
        "docker" -> build.requirements { docker() }
        else -> throw IllegalArgumentException("Invalid requirement: $name")
    }
}

fun Requirements.linux() {
    contains("teamcity.agent.jvm.os.name", "Linux")
}

fun Requirements.macos() {
    contains("teamcity.agent.jvm.os.name", "Mac OS X")
}

fun Requirements.windows() {
    contains("teamcity.agent.jvm.os.name", "Windows")
}

fun Requirements.docker() {
    exists("docker.server.version")
}
