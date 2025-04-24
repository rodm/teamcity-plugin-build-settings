
import extensions.defaultPluginBuildTemplate
import extensions.createVcsRoot
import extensions.createApiBuildConfigurations
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.DslContext
import jetbrains.buildServer.configs.kotlin.RelativeId
import jetbrains.buildServer.configs.kotlin.Requirements
import jetbrains.buildServer.configs.kotlin.project
import jetbrains.buildServer.configs.kotlin.version

version = "2025.03"

project {
    params {
        param("teamcity.ui.settings.readOnly", "true")
    }

    val vcsRoot = createVcsRoot()
    val buildTemplate = defaultPluginBuildTemplate(vcsRoot)

    val builds = createApiBuildConfigurations(buildTemplate)

    val gradleOptions = DslContext.getParameter("gradle.options", "")
    val reportCodeQuality = buildType {
        templates(buildTemplate)
        id("ReportCodeQuality")
        name = "Report - Code Quality"

        params {
            param("gradle.opts", "%sonar.opts% ${gradleOptions}")
            param("gradle.tasks", "clean build sonar")
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
        "solaris" -> build.requirements { solaris() }
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

fun Requirements.solaris() {
    contains("teamcity.agent.jvm.os.name", "SunOS")
}

fun Requirements.windows() {
    contains("teamcity.agent.jvm.os.name", "Windows")
}

fun Requirements.docker() {
    exists("docker.server.version")
}
