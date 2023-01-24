package no.elhub.tools.sonarphab

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

class SonarIssueTest : DescribeSpec({

    describe("parsing the Sonar JSON file") {

        val inputStream = withContext(Dispatchers.IO) {
            FileInputStream(File("src/test/resources/sonarIssues.json"))
        }
        val factory = ObjectMapper().getFactory()
        val parser = factory.createParser(inputStream)
        val issues = SonarIssue.retrieveIssues(parser)

        it("should contain 39 issues") {
            issues.size shouldBe 39
        }

        it("should contain 7 issues with rule issue kotlin:S1135") {
            issues.filter { it.rule.equals("kotlin:S1135") }.size shouldBe 7
        }

        it("should contain 2 issues with error severity") {
            issues.filter { it.severity == PhabricatorLintSeverity.ERROR }.size shouldBe 2
        }

        it("should contain 30 issues with warning severity") {
            issues.filter { it.severity == PhabricatorLintSeverity.WARNING }.size shouldBe 30
        }

        it("should contain 7 issues with advice severity") {
            issues.filter { it.severity == PhabricatorLintSeverity.ADVICE }.size shouldBe 7
        }

        it("should contain 11 issues from the pagecontroller component") {
            issues.filter { it.component.equals("elhub-ui-web-portal:app/view/main/PageController.js") }.size shouldBe 11
        }

        it("should contain 11 issues from the pagecontroller file") {
            issues.filter { it.fileName.equals("app/view/main/PageController.js") }.size shouldBe 11
        }

        it("should contain an issue from line 207 and character 8 of PageController.js") {
            issues.filter {
                it.fileName.equals("app/view/main/PageController.js") &&
                        it.line == 207 &&
                        it.char == 8
            }.size shouldBe 1
        }

        it("should contain 35 code smells") {
            issues.filter { it.type.equals("CODE_SMELL") }.size shouldBe 35
        }

        it("should contain 6 messages asking to remove commented out code") {
            issues.filter { it.message.equals("Remove this commented out code.") }.size shouldBe 6
        }

    }

    describe("parsing an empty Sonar JSON file") {

        val inputStream = withContext(Dispatchers.IO) {
            FileInputStream(File("src/test/resources/sonarEmptyIssues.json"))
        }
        val factory = ObjectMapper().getFactory()
        val parser = factory.createParser(inputStream)
        val issues = SonarIssue.retrieveIssues(parser)

        it("should contain 0 issues") {
            issues.size shouldBe 0
        }

    }

})
