import jetbrains.buildServer.configs.kotlin.v2019_2.version
import jetbrains.buildServer.configs.kotlin.v2019_2.project
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.ProjectFeature
import jetbrains.buildServer.configs.kotlin.v2019_2.ProjectFeatures
import jetbrains.buildServer.configs.kotlin.v2019_2.Template
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.VcsTrigger
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot
import no.elhub.common.build.configuration.CreateExeGradle

version = "2020.2"

project {

    params {
        param("teamcity.ui.settings.readOnly", "true")
    }

    buildType(
        CreateExeGradle(
            CreateExeGradle.Config(
                id = "CreateExe",
                name = "Assemble Executable",
                vcsRoot = DslContext.settingsRoot
            )
        )
    )

}
