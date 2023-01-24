package no.elhub.tools.sonarphab

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import org.apache.http.HttpStatus
import org.apache.http.StatusLine
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream

class ConduitClientTest : DescribeSpec({

    describe("A ConduitClient receiving correct results") {
        mockkConstructor(HttpClientBuilder::class)
        val mockHttpClient = mockk<CloseableHttpClient>()
        every { anyConstructed<HttpClientBuilder>().build() } returns mockHttpClient
        val mockCloseableResponse = mockk<CloseableHttpResponse>()
        val mockEntity = mockk<StringEntity>()
        every { mockCloseableResponse.entity } returns mockEntity
        val mockContent = "{\n" +
                "  \"objectId\": {\n" +
                "    \"phid\": \"PHID-XXXX-1111\"\n" +
                "  },\n" +
                "  \"data\": [\n" +
                "    {\n" +
                "      \"phid\": \"PHID-YYYY-1111\",\n" +
                "    },\n" +
                "    {\n" +
                "      \"phid\": \"PHID-YYYY-2222\",\n" +
                "    }\n" +
                "  ]," +
                " \"error_info\": \"null\",\n" +
                " \"error_code\": \"null\"\n" +
                "}\n"
        every { mockEntity.content } returns ByteArrayInputStream(mockContent.toByteArray(Charsets.UTF_8))
        val mockStatusLine = mockk<StatusLine>()
        every { mockCloseableResponse.statusLine } returns mockStatusLine
        every { mockStatusLine.statusCode } returns HttpStatus.SC_OK
        every { mockStatusLine.reasonPhrase } returns "This is an error"
        every { mockHttpClient.execute(any()) } returns mockCloseableResponse
        val conduit = ConduitClient("http://www.example.com", "DUMMY-TOKEN")

        it("postComment should receive a JsonObject") {
            conduit.postComment("D001", "Hasta la vista, baby!").get("data") is JSONObject
        }

    }

    describe("A ConduitClient not receiving a 200 status code") {
        mockkConstructor(HttpClientBuilder::class)
        val mockHttpClient = mockk<CloseableHttpClient>()
        every { anyConstructed<HttpClientBuilder>().build() } returns mockHttpClient
        val mockCloseableResponse = mockk<CloseableHttpResponse>()
        val mockEntity = mockk<StringEntity>()
        every { mockCloseableResponse.entity } returns mockEntity
        val mockContent = "{\n" +
                "  \"objectId\": {\n" +
                "    \"phid\": \"PHID-XXXX-1111\"\n" +
                "  }\n" +
                "}\n"
        every { mockEntity.content } returns ByteArrayInputStream(mockContent.toByteArray(Charsets.UTF_8))
        val mockStatusLine = mockk<StatusLine>()
        every { mockCloseableResponse.statusLine } returns mockStatusLine
        every { mockStatusLine.statusCode } returns HttpStatus.SC_BAD_REQUEST
        every { mockStatusLine.reasonPhrase } returns "This is an error"
        every { mockHttpClient.execute(any()) } returns mockCloseableResponse
        val conduit = ConduitClient("http://www.example.com", "DUMMY-TOKEN")

        it("postComment should throw an exception") {
            shouldThrow<ConduitClientException> {
                conduit.postComment("D001", "Hasta la vista, baby!")
            }
        }

    }

    describe("A ConduitClient receiving a non-null error_code") {
        mockkConstructor(HttpClientBuilder::class)
        val mockHttpClient = mockk<CloseableHttpClient>()
        every { anyConstructed<HttpClientBuilder>().build() } returns mockHttpClient
        val mockCloseableResponse = mockk<CloseableHttpResponse>()
        val mockEntity = mockk<StringEntity>()
        every { mockCloseableResponse.entity } returns mockEntity
        val mockContent = "{\n" +
                "  \"objectId\": {\n" +
                "    \"phid\": \"PHID-XXXX-1111\"\n" +
                "  },\n" +
                " \"error_info\": \"There is an error\",\n" +
                " \"error_code\": \"ERR-001\"\n" +
                "}\n"
        every { mockEntity.content } returns ByteArrayInputStream(mockContent.toByteArray(Charsets.UTF_8))
        val mockStatusLine = mockk<StatusLine>()
        every { mockCloseableResponse.statusLine } returns mockStatusLine
        every { mockStatusLine.statusCode } returns HttpStatus.SC_BAD_REQUEST
        every { mockStatusLine.reasonPhrase } returns "This is an error"
        every { mockHttpClient.execute(any()) } returns mockCloseableResponse
        val conduit = ConduitClient("http://www.example.com", "DUMMY-TOKEN")

        it("postComment should throw an exception") {
            shouldThrow<ConduitClientException> {
                conduit.postComment("D001", "Hasta la vista, baby!")
            }
        }

    }

    describe("A ConduitClient for lint objects") {
        mockkConstructor(HttpClientBuilder::class)
        val mockHttpClient = mockk<CloseableHttpClient>()
        every { anyConstructed<HttpClientBuilder>().build() } returns mockHttpClient
        val mockCloseableResponse = mockk<CloseableHttpResponse>()
        val mockEntity = mockk<StringEntity>()
        every { mockCloseableResponse.entity } returns mockEntity
        val mockContent = "{\n" +
                "  \"objectId\": {\n" +
                "    \"phid\": \"PHID-XXXX-1111\"\n" +
                "  },\n" +
                "  \"data\": [\n" +
                "    {\n" +
                "      \"phid\": \"PHID-YYYY-1111\",\n" +
                "    },\n" +
                "    {\n" +
                "      \"phid\": \"PHID-YYYY-2222\",\n" +
                "    }\n" +
                "  ]," +
                " \"error_info\": \"null\",\n" +
                " \"error_code\": \"null\"\n" +
                "}\n"
        every { mockEntity.content } returns ByteArrayInputStream(mockContent.toByteArray(Charsets.UTF_8))
        val mockStatusLine = mockk<StatusLine>()
        every { mockCloseableResponse.statusLine } returns mockStatusLine
        every { mockStatusLine.statusCode } returns HttpStatus.SC_OK
        every { mockStatusLine.reasonPhrase } returns "This is an error"
        every { mockHttpClient.execute(any()) } returns mockCloseableResponse
        val conduit = ConduitClient("http://www.example.com", "DUMMY-TOKEN")

        it("sendLintResults should receive a JsonObject") {
            conduit.sendLintResults(JSONArray()).get("data") is JSONObject
        }

    }

})
