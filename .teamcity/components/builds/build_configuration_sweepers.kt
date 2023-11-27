package builds

import DefaultBuildTimeoutDuration
import DefaultParallelism
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot


fun buildConfigurationForSweeper(sweeperName: String, packages: Map<String, Map<String, String>>, vcsRoot: GitVcsRoot, environmentVariables: AccTestConfiguration): BuildType {
    val sweeperPackage: Map<String, String> = packages.getValue("sweeper")
    val sweeperPath: String = sweeperPackage.getValue("path").toString()
    val s = SweeperDetails()

    return s.sweeperBuildConfig(sweeperName, sweeperPath, vcsRoot, DefaultParallelism, environmentVariables)
}

class SweeperDetails() {

    fun sweeperBuildConfig(
        sweeperName: String,
        path: String,
        vcsRoot: GitVcsRoot,
        parallelism: Int,
        environmentVariables: AccTestConfiguration,
        buildTimeout: Int = DefaultBuildTimeoutDuration
    ): BuildType {

        // These hardcoded values affect the sweeper CLI command's behaviour
        val testPrefix = "TestAcc"
        val testTimeout = "12"
        val sweeperRegions = "us-central1"
        val sweeperRun = "" // Empty string means all sweepers run

        return BuildType {

            id(createID(sweeperName))

            name = sweeperName

            vcs {
                root(vcsRoot)
                cleanCheckout = true
            }

            steps {
                setGitCommitBuildId()
                tagBuildToIndicatePurpose()
                configureGoEnv()
                downloadTerraformBinary()
                runSweepers(sweeperName)
            }

            features {
                golang()
            }

            params {
                configureGoogleSpecificTestParameters(environmentVariables)
                terraformAcceptanceTestParameters(parallelism, testPrefix, testTimeout)
                terraformSweeperParameters(sweeperRegions, sweeperRun)
                terraformLoggingParameters()
                terraformCoreBinaryTesting()
                terraformShouldPanicForSchemaErrors()
                readOnlySettings()
                workingDirectory(path)
            }

            artifactRules = "%teamcity.build.checkoutDir%/debug*.txt"

            failureConditions {
                errorMessage = true
                executionTimeoutMin = buildTimeout
            }

            // NOTE: dependencies and triggers are added by methods after the BuildType object is created
        }
    }

    private fun createID(name: String): String {
        // Replacing chars can be necessary, due to limitations on IDs
        // "ID should start with a latin letter and contain only latin letters, digits and underscores (at most 225 characters)."
        return name.replace("-", "_").uppercase()
    }
}
