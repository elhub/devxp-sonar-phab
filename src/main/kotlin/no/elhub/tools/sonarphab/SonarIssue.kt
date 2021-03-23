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

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.JsonNode

/** Model for a Sonar Issue
 */
class SonarIssue(parser: JsonParser) {
    var rule = ""
    var severity = PhabricatorLintSeverity.ADVICE
    var component = ""
    var fileName = ""
    var line = 0
    var char = 0
    var type = ""
    var message = ""

    init {
        parser.nextToken() // START_OBJECT
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            when (parser.currentName) {
                "key" -> parser.text // Weirdness: I'd expect nextTextValue here, but for some reason, that breaks the parsing. Not sure why!
                "rule" -> rule = parser.nextTextValue()
                "severity" -> {
                    when (parser.nextTextValue()) {
                        "BLOCKER", "CRITICAL" -> severity = PhabricatorLintSeverity.ERROR
                        "MAJOR", "MINOR" -> severity = PhabricatorLintSeverity.WARNING
                        "INFO" -> severity = PhabricatorLintSeverity.ADVICE
                    }
                }
                "component" -> {
                    component = parser.nextTextValue()
                    fileName = component.substringAfterLast(":")
                }
                "project" -> parser.nextTextValue()
                "line" -> line = parser.nextIntValue(0)
                "hash" -> parser.nextTextValue()
                "textRange" -> {
                    parser.nextToken() // START_OBJECT
                    while (parser.nextToken() != JsonToken.END_OBJECT) {
                        when (parser.currentName) {
                            "startLine" -> line = parser.nextIntValue(0)
                            "startOffset" -> char = parser.nextIntValue(0)
                            else -> parser.nextIntValue(0)
                        }
                    }
                }
                "flows" -> {
                    parser.nextToken() // START_ARRAY
                    var closures = 0
                    while (parser.nextToken() != JsonToken.END_ARRAY || closures > 0) {
                        if (parser.currentToken == JsonToken.START_ARRAY)
                            closures++
                        if (parser.currentToken == JsonToken.END_ARRAY)
                            closures--
                    }
                }
                "status" -> parser.nextTextValue()
                "message" -> message = parser.nextTextValue()
                "effort" -> parser.nextTextValue()
                "debt" -> parser.nextTextValue()
                "tags" -> {
                    parser.nextToken() // START_ARRAY
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        // NOOP
                    }
                }
                "type" -> type = parser.nextTextValue()
                else -> parser.nextToken()
            }
        }
    }

    companion object {

        /** Retrieve issues from Sonar server and parse them into the issues list
         */
        fun retrieveIssues(parser: JsonParser): ArrayList<SonarIssue> {
            val issues = ArrayList<SonarIssue>()
            val startToken = parser.nextToken()
            if (startToken != JsonToken.START_OBJECT)
                throw SonarPhabException("Malformed sonar scan file.")
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                val fieldName = parser.currentName
                when (fieldName) {
                    "total" -> {
                        val issuesNo = parser.nextIntValue(0)
                        println("Found $issuesNo issues")
                        if (issuesNo < 1) {
                            // Write to phabricator
                            return ArrayList()
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
            return issues
        }

    }

}
