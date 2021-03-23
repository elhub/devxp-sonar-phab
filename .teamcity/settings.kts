import jetbrains.buildServer.configs.kotlin.v2019_2.version
import jetbrains.buildServer.configs.kotlin.v2019_2.project
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.ProjectFeature
import jetbrains.buildServer.configs.kotlin.v2019_2.ProjectFeatures
import jetbrains.buildServer.configs.kotlin.v2019_2.sequential
import jetbrains.buildServer.configs.kotlin.v2019_2.Template
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.VcsTrigger
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot
import no.elhub.common.build.configuration.AssembleGradleExecutable
import no.elhub.common.build.configuration.SonarScan
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
                    vcsRoot = DslContext.settingsRoot
                )
            )
        )

        buildType(
            SonarScan(
                SonarScan.Config(
                    vcsRoot = DslContext.settingsRoot,
                    sonarId = "no.elhub.tools.sonarphab:dev-tools-sonar-phab",
                    sonarProjectSources = "src"
                )
            )
        )

        buildType(
            AssembleGradleExecutable(
                AssembleGradleExecutable.Config(
                    vcsRoot = DslContext.settingsRoot
                )
            )
        )

    }

    buildChain.buildTypes().forEach { buildType(it) }
}
