
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.project
import jetbrains.buildServer.configs.kotlin.v2019_2.toId
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot.AgentCheckoutPolicy.NO_MIRRORS
import jetbrains.buildServer.configs.kotlin.v2019_2.version

version = "2021.2"

project {

    val vcsName = DslContext.getParameter("vcs.name")
    val vcsUrl = DslContext.getParameter("vcs.url")
    val vcsRoot = GitVcsRoot {
        id(vcsName.toId())
        name = vcsName
        url = vcsUrl
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

    buildTypesOrder = builds.toList()
}
