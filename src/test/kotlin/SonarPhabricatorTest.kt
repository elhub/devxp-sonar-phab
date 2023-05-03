package no.elhub.tools.sonarphab

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import picocli.CommandLine
import java.io.File
import java.io.FileInputStream
import java.io.PrintWriter
import java.io.StringWriter

class SonarPhabricatorTest : DescribeSpec({


    describe("A running sonar-phab application") {
        val app = SonarPhabricator()
        val cmd = CommandLine(app)
        val sw = StringWriter()
        cmd.out = PrintWriter(sw)

        it("calling --help should return a help message") {
            val exitCode = cmd.execute("--help")
            exitCode shouldBe 0
            sw.toString() shouldStartWith "Usage: sonar-phab"
        }

        /*
        it("calling method without connection should throw exit exception") {
            withEnvironment(
                mapOf(
                    "SONAR_RESULTS" to "src/test/resources/report-task.txt",
                    "SONAR_TOKEN" to "DUMMY_TOKEN",
                    "PHABRICATOR_URI" to "https://phabricator.example.com",
                    "PHABRICATOR_CONDUIT_TOKEN" to "DUMMY_TOKEN",
                    "PHABRICATOR_HARBORMASTER_PHID" to "phid_dummy_id"
                )
            ) {
                main(arrayOf())
            }

        }
         */

    }

    describe("Loading properties from a report-task file") {
        loadProperties("src/test/resources/report-task.txt")

        it("should parse the properties correctly") {
            taskResultUri shouldBe "http://example.com?task=001"
            sonarUrl shouldBe "http://sonar.example.com"
            sonarId shouldBe "DevXpSonarPhab"
            sonarBranch shouldBe "D0001"
        }

    }

    describe("Polling the sonar server") {
        mockkStatic(::openSonarConnection)
        val mockContent = "{\n" +
            "  \"task\": {\n" +
            "    \"organization\": \"my-org-1\",\n" +
            "    \"id\": \"AVAn5RKqYwETbXvgas-I\",\n" +
            "    \"type\": \"REPORT\",\n" +
            "    \"componentId\": \"AVAn5RJmYwETbXvgas-H\",\n" +
            "    \"componentKey\": \"project_1\",\n" +
            "    \"componentName\": \"Project One\",\n" +
            "    \"componentQualifier\": \"TRK\",\n" +
            "    \"analysisId\": \"123456\",\n" +
            "    \"status\": \"SUCCESS\",\n" +
            "    \"submittedAt\": \"2015-10-02T11:32:15+0200\",\n" +
            "    \"startedAt\": \"2015-10-02T11:32:16+0200\",\n" +
            "    \"executedAt\": \"2015-10-02T11:32:22+0200\",\n" +
            "    \"executionTimeMs\": 5286,\n" +
            "    \"logs\": false,\n" +
            "    \"scannerContext\": \"SonarQube plugins:\\n\\t- Git 1.0 (scmgit)\\n\\t- Java 3.13.1 (java)\",\n" +
            "    \"hasScannerContext\": true\n" +
            "  }\n" +
            "}\n"
        every { openSonarConnection(any()) } returns mockContent.byteInputStream()
        val res = pollSonarServer("http://sonar.example.com/api/ce/task")

        it("should return with success") {
            res shouldBe true
        }

    }

    describe("Write to Phabricator with no issues") {
        val conduit = mockk<ConduitClient>()
        every { conduit.postComment(any(), any()) } returns JSONObject()
        every { conduit.sendLintResults(any()) } returns JSONObject()

        it("should return 0 issues if there are none") {
            issues = ArrayList()
            val res = writeToPhabricator(conduit)
            res shouldBe 0
        }

        it("should return the number of issues that exist") {
            val inputStream = withContext(Dispatchers.IO) {
                FileInputStream(File("src/test/resources/sonarIssues.json"))
            }
            val factory = ObjectMapper().getFactory()
            val parser = factory.createParser(inputStream)
            issues = SonarIssue.retrieveIssues(parser)
            writeToPhabricator(conduit)
            issues.size shouldBe 39
        }
    }


})
