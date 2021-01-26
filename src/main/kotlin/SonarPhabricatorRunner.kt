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
package no.elhub.dev.tools

import picocli.CommandLine
import java.io.File
import kotlin.system.exitProcess
import java.io.FileInputStream
import java.lang.Thread.sleep
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import java.time.ZonedDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Properties


const val helpMessage = """
sonar-phab uses a number of environment variables in order to run correctly;
these must be set to process the changeset correctly.

SONAR_URI: The URI for the Sonar server.
SONAR_BRANCH: The Diff ID to be processed from Phabricator. This becomes a
branch in the Sonar analysis.
PHABRICATOR_URI: The URI for the Phabricator server.
PHABRICATOR_HARBORMASTER_PHID: The phid of Harbormaster in Phabricator.
"""
val sonarUrl = System.getenv("SONAR_URI")
val sonarBranch = System.getenv("PHABRICATOR_REVISION_ID")
val phabricatorUrl = System.getenv("PHABRICATOR_URI")
val targetPhid = System.getenv("PHABRICATOR_HARBORMASTER_PHID")
val conduitToken = System.getenv("PHABRICATOR_CONDUIT_TOKEN")
var sonarId = ""
val issues = ArrayList<SonarIssue>()

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

class SonarPhabricatorRunner : Runnable {
    @CommandLine.Option(names = ["-h", "--help"], usageHelp = true, description = ["output usage information"])
    var help = false

    /**
     * Application entry point for sonar-phab
     *
     * @param args an array of String arguments to be parsed
     */
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            CommandLine(SonarPhabricatorRunner()).execute(*args)
        }
    }

    override fun run() {
        if (help) {
            println(helpMessage)
            exitProcess(0)
        }
        println("Start processing")
        pollSonarServer()
        retrieveIssues()
        writeToPhabricator(ConduitClient(phabricatorUrl, conduitToken))
        exitProcess(0)
    }

}

/** Poll the Sonar server for a specified amount of time (currently ~60 secs)
 */
fun pollSonarServer() {
    val properties = Properties()
    val inputStream = FileInputStream("build/sonar/report-task.txt")
    properties.load(inputStream)
    val taskResultUri = properties["ceTaskUrl"].toString()
    print("Waiting on task to complete (task $taskResultUri)")
    var success = false
    var iterations = 0
    while (!success || iterations > 120) {
        val factory = JsonFactory()
        val parser = factory.createParser(URL(taskResultUri))
        parser.nextToken() // JsonToken.START_OBJECT
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            val fieldName = parser.currentName
            when (fieldName) {
                "task" -> {
                    parser.nextToken() // JsonToken.START_OBJECT
                    while (parser.nextToken() != JsonToken.END_OBJECT) {
                        val taskField = parser.currentName
                        when (taskField) {
                            "status" -> {
                                val result = parser.nextTextValue()
                                if (result.equals("SUCCESS"))
                                    success = true
                            }
                            else -> {
                                // NOOP
                            }
                        }
                    }
                }
                else -> {
                    // NOOP
                }
            }
        }
        iterations++
        print(".")
        sleep(500)
    }
    println(".")
}

/** Retrieve issues from Sonar server and parse them into the issues list
 */
fun retrieveIssues() {
    val factory = JsonFactory()
    val sonarResult = "$sonarUrl/api/issues/search?componentKeys=$sonarId&branch=$sonarBranch&resolved=false&facets=severities"
    println("Retrieving $sonarResult")
    val parser = factory.createParser(URL(sonarResult))
    val startToken = parser.nextToken()
    if (startToken != JsonToken.START_OBJECT)
        throw RuntimeException("Malformed sonar scan file.")
    while (parser.nextToken() != JsonToken.END_OBJECT) {
        val fieldName = parser.currentName
        when (fieldName) {
            "total" -> {
                val issuesNo = parser.nextIntValue(0)
                println("Found $issuesNo issues")
                if (issuesNo < 1) {
                    // Write to phabricator
                    return
                }
            }
            "paging" -> {
                parser.nextToken() // START_OBJECT
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    // NOOP
                }
            }
            "issues" -> {
                parser.nextToken() // START_ARRAY
                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    issues.add(SonarIssue(parser))
                }
            }
            else -> {
                // NOOP
            }
        }
    }
}

fun writeToPhabricator(conduitClient: ConduitClient) {
    if (issues.isEmpty())
        conduitClient.postComment(
            sonarBranch,
            "SonarQube did not find any issues. Great job!\n" +
                    "Full sonar scan: $sonarUrl/dashboard?id=$sonarId&branch=$sonarBranch&resolved=false"
        )

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
        "SonarQube identified ${issues.size} issues. $errors errors, $warnings warnings, and $advice advice. See lint results.\n" +
                "Full sonar scan: $sonarUrl/dashboard?id=$sonarId&branch=$sonarBranch&resolved=false"
    )
}
