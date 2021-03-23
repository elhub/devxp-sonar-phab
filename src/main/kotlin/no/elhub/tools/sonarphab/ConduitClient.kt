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

import org.apache.commons.io.IOUtils
import org.apache.http.NameValuePair
import org.apache.http.client.methods.HttpPost
import org.apache.http.HttpStatus
import org.apache.http.impl.client.HttpClientBuilder
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

class ConduitClient(var url: String, var token: String) {

    fun perform(action: String, params: JSONObject): JSONObject {
        val httpClient = HttpClientBuilder.create().build()
        val postRequest = makeRequest(action, params)
        val response = httpClient.execute(postRequest)
        val responseBody = IOUtils.toString(response.entity.content, Charsets.UTF_8)
        if (response.statusLine.statusCode != HttpStatus.SC_OK) {
            throw ConduitClientException(response.statusLine.reasonPhrase)
        }
        val result = JSONObject(responseBody)
        val errorInfo = result.get("error_info").toString()
        if (!(result.get("error_code").toString().equals("null")
                    && errorInfo.equals("null"))
        ) {
            throw ConduitClientException(errorInfo)
        }
        return result
    }

    private fun makeRequest(action: String, params: JSONObject): HttpPost {
        val postRequest = HttpPost(URL(URL(URL(url), "/api/"), action).toURI())
        val conduitMetadata = JSONObject()
        conduitMetadata.put("token", token)
        params.put("__conduit__", conduitMetadata)
        val formData = ArrayList<NameValuePair>()
        formData.add(org.apache.http.message.BasicNameValuePair("params", params.toString()))
        val entity = org.apache.http.client.entity.UrlEncodedFormEntity(formData, "UTF-8")
        postRequest.setEntity(entity)
        return postRequest
    }

    fun postComment(revisionID: String, message: String): JSONObject {
        val params = JSONObject()
            .put("objectIdentifier", revisionID)
            .put(
                "transactions",
                JSONArray().put(
                    JSONObject()
                        .put("type", "comment")
                        .put("value", message)
                )
            )
        return perform("differential.revision.edit", params)
    }
}

