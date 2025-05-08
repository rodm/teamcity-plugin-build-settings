
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.project
import jetbrains.buildServer.configs.kotlin.toId
import jetbrains.buildServer.configs.kotlin.version
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot.AgentCheckoutPolicy.NO_MIRRORS

version = "2025.03"

project {
    params {
        param("teamcity.ui.settings.readOnly", "true")
    }

    val vcsRoot = GitVcsRoot {
        id("teamcity-plugin-build".toId())
        name = "teamcity-plugin-build"
        url = "https://github.com/rodm/teamcity-plugin-build-settings.git"
        branch = "refs/heads/main"
        checkoutPolicy = NO_MIRRORS
    }
    vcsRoot(vcsRoot)

    val template = template {
        id("Build")
        name = "Build"

        params {
            param("gradle.opts", "")
            param("gradle.tasks", "clean build")
        }

        vcs {
            root(vcsRoot)
        }

        steps {
            gradle {
                tasks = "%gradle.tasks%"
                buildFile = ""
                gradleParams = "%gradle.opts%"
                enableStacktrace = true
                jdkHome = "%java8.home%"
            }
        }

        triggers {
            vcs {
                triggerRules = """
                -:.github/**
                -:README.adoc
                """.trimIndent()
            }
        }

        features {
            perfmon {
                id = "perfmon"
            }
        }
    }


    val builds = mutableListOf<BuildType>()
    val build = buildType {
        id("Build1")
        templates(template)
        name = "Build - Settings"

        params {
            param("gradle.opts", "-Pteamcity.server.url=%teamcity.serverUrl%/app/dsl-plugins-repository")
        }
    }
    builds.add(build)

    val report = buildType {
        id("Report")
        templates(template)
        name = "Report - Code Quality"

        params {
            param("gradle.opts", "-Pteamcity.server.url=%teamcity.serverUrl%/app/dsl-plugins-repository %sonar.opts%")
            param("gradle.tasks", "clean build sonar")
        }
    }
    builds.add(report)

    buildTypesOrder = builds.toList()
}
