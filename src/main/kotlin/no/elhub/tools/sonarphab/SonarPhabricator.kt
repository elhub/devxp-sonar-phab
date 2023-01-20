/*
 * Copyright (c) 2020 Elhub A/S
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package no.elhub.tools.sonarphab

import picocli.CommandLine
import java.io.File
import kotlin.system.exitProcess
import java.io.FileInputStream
import java.lang.Thread.sleep
import java.net.URL
import java.net.HttpURLConnection
import org.json.JSONArray
import org.json.JSONObject
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.JsonNode
import java.time.ZonedDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Properties
import no.elhub.tools.sonarphab.SonarPhabException
import java.net.URLConnection
import java.util.Base64
import java.util.concurrent.Callable

const val POLL_ITERATIONS = 120 // Number of times to poll for a response
const val POLL_SLEEP: Long = 500 // Time to wait between polls for a response
const val HELP_MESSAGE = """
sonar-phab uses a number of environment variables in order to run correctly;
these must be set to process the changeset correctly.

SONAR_RESULTS: Sonar report task summary file (report-task.txt)
PHABRICATOR_URI: The URI for the Phabricator server.
PHABRICATOR_HARBORMASTER_PHID: The phid of Harbormaster in Phabricator.
PHABRICATOR_CONDUIT_TOKEN: The conduit token of the user that the process will
run as.
"""
val sonarResults = System.getenv("SONAR_RESULTS") ?: "build/sonar/report-task.txt"  // Assumes a gradle run
var taskResultUri = ""
var sonarUrl = ""
var sonarBranch = ""
var sonarId = ""
var sonarToken = System.getenv("SONAR_TOKEN")
val sonarAuth = "Basic " + Base64.getEncoder().encodeToString("$sonarToken:".toByteArray())
val phabricatorUrl = System.getenv("PHABRICATOR_URI")
val targetPhid = System.getenv("PHABRICATOR_HARBORMASTER_PHID")
val conduitToken = System.getenv("PHABRICATOR_CONDUIT_TOKEN")
var issues = ArrayList<SonarIssue>()

/**
 * Class for handling console arguments.
 */
@CommandLine.Command(
        name = "sonar-phab",
        description = ["\nTool for retrieving sonar scans from SonarQube and posting them to Phabricator"],
        optionListHeading = "@|bold %nOptions|@:%n",
        sortOptions = false,
        footer = [
            "\nDeveloped by Elhub"
        ]
)
class SonarPhabricator : Callable<Int> {
    @CommandLine.Option(
            names = ["-h", "--help"],
            usageHelp = true,
            description = ["output usage information"]
    )
    var help = false

    override fun call(): Int {
        if (help) {
            println(HELP_MESSAGE)
            return 0
        }
        println("Start processing")
        loadProperties()
        pollSonarServer()
        val factory = ObjectMapper().getFactory()
        val sonarResultUri = "$sonarUrl/api/issues/search?componentKeys=$sonarId&inNewCodePeriod=true&branch=$sonarBranch" +
                "&resolved=false&facets=severities"
        val sonarConnection = URL(sonarResultUri).openConnection()
        sonarConnection.setRequestProperty("Authorization", sonarAuth)
        val parser = factory.createParser(sonarConnection.getInputStream())
        issues = SonarIssue.retrieveIssues(parser)
        writeToPhabricator(ConduitClient(phabricatorUrl, conduitToken))
        return 0
    }

}

fun loadProperties() {
    val properties = Properties()
    val inputStream = FileInputStream(sonarResults)
    properties.load(inputStream)
    taskResultUri = properties["ceTaskUrl"].toString()
    sonarUrl = properties["serverUrl"].toString()
    sonarId = properties["projectKey"].toString()
    sonarBranch = properties["branch"].toString()
    println("$taskResultUri")
}

/** Poll the Sonar server for a specified amount of time (currently ~60 secs)
 */
fun pollSonarServer() {
    print("Waiting on task to complete (task $taskResultUri)")
    var success = false
    var iterations = 0
    while (!success || iterations > POLL_ITERATIONS) {
        val factory = JsonFactory()
        val taskConnection = URL(taskResultUri).openConnection()
        taskConnection.setRequestProperty("Authorization", sonarAuth)
        val parser = factory.createParser(taskConnection.getInputStream())
        parser.nextToken() // JsonToken.START_OBJECT
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            val fieldName = parser.currentName
            when (fieldName) {
                "task" -> success = parseTask(parser)
                else -> Unit // NOOP
            }
        }
        iterations++
        print(".")
        sleep(POLL_SLEEP)
    }
    println(".")
}

private fun parseTask(parser: JsonParser): Boolean {
    parser.nextToken() // JsonToken.START_OBJECT
    while (parser.nextToken() != JsonToken.END_OBJECT) {
        val taskField = parser.currentName
        when (taskField) {
            "status" -> {
                val result = parser.nextTextValue()
                if (result.equals("SUCCESS"))
                    return true
            }

            else -> {
                // NOOP
            }
        }
    }
    return false
}

fun writeToPhabricator(conduitClient: ConduitClient) {
    if (issues.isEmpty()) {
        conduitClient.postComment(
                sonarBranch,
                "SonarQube did not find any issues. Great job!\n" +
                        "Full sonar scan: $sonarUrl/dashboard?id=$sonarId&branch=$sonarBranch&resolved=false"
        )
        return
    }
    val lintResults = JSONArray()
    var errors = 0
    var warnings = 0
    var advice = 0
    issues.forEach {
        println("${it.fileName}: ${it.line} ${it.char}: ${it.message}")
        when (it.severity) {
            PhabricatorLintSeverity.ERROR -> errors++
            PhabricatorLintSeverity.WARNING -> warnings++
            PhabricatorLintSeverity.ADVICE -> advice++
        }
        lintResults.put(
                JSONObject()
                        .put("name", it.message)
                        .put("code", it.rule)
                        .put("severity", it.severity)
                        .put("path", it.fileName)
                        .put("line", it.line)
                        .put("char", it.char)
        )
    }
    val params = JSONObject()
            .put("buildTargetPHID", targetPhid)
            .put("type", "work")
            .put("lint", lintResults)
    conduitClient.perform("harbormaster.sendmessage", params)
    conduitClient.postComment(
            sonarBranch,
            "SonarQube identified ${issues.size} issues. $errors errors, $warnings warnings, and $advice advice. " +
                    "See lint results.\n" +
                    "Full sonar scan: $sonarUrl/dashboard?id=$sonarId&branch=$sonarBranch&resolved=false"
    )
}

fun main(args: Array<String>): Unit = exitProcess(CommandLine(SonarPhabricator()).execute(*args))
