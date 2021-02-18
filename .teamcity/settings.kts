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
import no.elhub.common.build.configuration.UnitTestGradle

version = "2020.2"

project {

    params {
        param("teamcity.ui.settings.readOnly", "true")
    }

    val buildChain = sequential {

        buildType(
            UnitTestGradle(
                UnitTestGradle.Config(
                    id = "UnitTest",
                    name = "Unit Test"
                )
            )
        )

        buildType(
            SonarScan(
                SonarScan.Config(
                    id = "SonarScan",
                    name = "Code Analysis",
                    vcsRoot = DslContext.settingsRoot,
                    sonarId = "dev-tools-sonar-phab",
                    sonarProjectSources = "app"
                )
            )
        )

        buildType(
            AssembleGradleExecutable(
                AssembleGradleExecutable.Config(
                    id = "AssembleGradleExecutable",
                    name = "Assemble",
                    vcsRoot = DslContext.settingsRoot
                )
            )
        )

    }

    buildChain.buildTypes().forEach { buildType(it) }
}
