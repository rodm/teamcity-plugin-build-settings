
import extensions.defaultPluginBuildTemplate
import extensions.createVcsRoot
import extensions.createApiBuildConfigurations
import extensions.createReportBuildConfiguration
import extensions.configureRequirements
import jetbrains.buildServer.configs.kotlin.project
import jetbrains.buildServer.configs.kotlin.version

version = "2025.11"

project {
    params {
        param("teamcity.ui.settings.readOnly", "true")
    }

    val vcsRoot = createVcsRoot()
    val buildTemplate = defaultPluginBuildTemplate(vcsRoot)

    val builds = createApiBuildConfigurations(buildTemplate)

    val reportCodeQuality = createReportBuildConfiguration(buildTemplate)
    builds.add(reportCodeQuality)

    configureRequirements(builds)

    buildTypesOrder = builds.toList()
}

