/*
 * Copyright 2015-2024 Ritense BV, the Netherlands.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ritense.openproduct.client

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.ritense.tokenauthentication.plugin.TokenAuthenticationPlugin
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import java.util.UUID

class OpenProductClientTest {
    private lateinit var server: MockWebServer
    private lateinit var baseUrl: String
    private val client = OpenProductClient()
    private val authentication = TokenAuthenticationPlugin().apply { token = "test-token" }
    private val logAppender = ListAppender<ILoggingEvent>()

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        baseUrl = server.url("/").toString().removeSuffix("/")
        logAppender.start()
        (LoggerFactory.getLogger(OpenProductClient::class.java) as Logger).addAppender(logAppender)
    }

    @AfterEach
    fun tearDown() {
        (LoggerFactory.getLogger(OpenProductClient::class.java) as Logger).detachAppender(logAppender)
        server.shutdown()
    }

    @Test
    fun `should call the configured base url with the configured token`() {
        server.enqueue(
            json("""{"uuid": "$PRODUCTTYPE_UUID", "naam": "Parkeervergunning", "dataobject_schema": null}"""),
        )

        client.getProducttype(baseUrl, authentication, PRODUCTTYPE_UUID)

        val request = server.takeRequest()
        assertThat(request.path).isEqualTo("/producttypen/api/v1/producttypen/$PRODUCTTYPE_UUID")
        assertThat(request.getHeader("Authorization")).isEqualTo("Token test-token")
    }

    @Test
    fun `should call the configured base url when it ends with a slash`() {
        server.enqueue(
            json("""{"uuid": "$PRODUCTTYPE_UUID", "naam": "Parkeervergunning", "dataobject_schema": null}"""),
        )

        client.getProducttype("$baseUrl/", authentication, PRODUCTTYPE_UUID)

        assertThat(server.takeRequest().path).isEqualTo("/producttypen/api/v1/producttypen/$PRODUCTTYPE_UUID")
    }

    @Test
    fun `should return the product types of every result page`() {
        server.enqueue(
            json(
                """{"count": 2, "next": "http://other-host/producttypen/api/v1/producttypen?page=2",
                   "results": [{"uuid": "$PRODUCTTYPE_UUID", "naam": "Parkeervergunning", "code": "PV"}]}""",
            ),
        )
        server.enqueue(
            json("""{"count": 2, "next": null, "results": [{"uuid": "$OTHER_UUID", "naam": "Hondenbelasting"}]}"""),
        )

        val producttypen = client.getProducttypen(baseUrl, authentication)

        assertThat(producttypen.map { it.naam }).containsExactly("Parkeervergunning", "Hondenbelasting")
        assertThat(server.takeRequest().path).isEqualTo("/producttypen/api/v1/producttypen")
        assertThat(server.takeRequest().path).isEqualTo("/producttypen/api/v1/producttypen?page=2")
    }

    @Test
    fun `should return the dataobject schema of a product type`() {
        server.enqueue(
            json(
                """{"uuid": "$PRODUCTTYPE_UUID", "naam": "Parkeervergunning",
                   "dataobject_schema": {"naam": "data", "schema": {"type": "object", "properties": {"kenteken": {"type": "string"}}}}}""",
            ),
        )

        val producttype = client.getProducttype(baseUrl, authentication, PRODUCTTYPE_UUID)

        assertThat(producttype.dataobjectSchema?.schema?.get("properties")).isEqualTo(
            mapOf(
                "kenteken" to mapOf("type" to "string"),
            ),
        )
    }

    @Test
    fun `should find the owner's products of a product type across every result page`() {
        server.enqueue(
            json(
                """{"count": 2, "next": "$baseUrl/producten/api/v1/producten?eigenaren__kvk_nummer=12345678&page=2&producttype__uuid=$PRODUCTTYPE_UUID",
                   "results": [{"uuid": "$PRODUCT_UUID", "status": "in_aanvraag", "prijs": null, "frequentie": ""}]}""",
            ),
        )
        server.enqueue(json("""{"count": 2, "next": null, "results": [{"uuid": "$OTHER_UUID", "status": "actief"}]}"""))

        val producten =
            client.findProducten(
                baseUrl,
                authentication,
                PRODUCTTYPE_UUID,
                EigenaarRequest(kvkNummer = "12345678", vestigingsnummer = "000012345678"),
            )

        assertThat(producten.map { it.status }).containsExactly("in_aanvraag", "actief")
        val firstPage = server.takeRequest().requestUrl!!
        assertThat(firstPage.encodedPath).isEqualTo("/producten/api/v1/producten")
        assertThat(firstPage.queryParameter("producttype__uuid")).isEqualTo(PRODUCTTYPE_UUID)
        assertThat(firstPage.queryParameter("eigenaren__kvk_nummer")).isEqualTo("12345678")
        assertThat(firstPage.queryParameter("eigenaren__vestigingsnummer")).isEqualTo("000012345678")
        assertThat(firstPage.queryParameter("eigenaren__bsn")).isNull()
        assertThat(server.takeRequest().requestUrl!!.queryParameter("page")).isEqualTo("2")
    }

    @Test
    fun `should log a failed call and raise it with the operation, status and Open Producten's error`() {
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"eigenaren": ["Dit veld is vereist."]}"""))

        val exception =
            assertThrows<HttpClientErrorException.BadRequest> {
                client.postProduct(baseUrl, authentication, mapOf("producttype_uuid" to PRODUCTTYPE_UUID))
            }

        assertThat(exception.statusCode.value()).isEqualTo(400)
        assertThat(exception.message)
            .contains("product aanmaken")
            .contains("400")
            .contains("Dit veld is vereist.")
        val logged = logAppender.list.single().formattedMessage
        assertThat(logged)
            .contains("POST $baseUrl/producten/api/v1/producten")
            .contains("400")
            .contains("Dit veld is vereist.")
    }

    @Test
    fun `should raise a failed call of an existing action with a readable message`() {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"detail": "Niet gevonden."}"""))

        val exception =
            assertThrows<HttpClientErrorException.NotFound> {
                client.getProduct(baseUrl, authentication, PRODUCT_UUID)
            }

        assertThat(exception.message).contains("product ophalen").contains("404").contains("Niet gevonden.")
        assertThat(
            logAppender.list.single().formattedMessage,
        ).contains("GET $baseUrl/producten/api/v1/producten/$PRODUCT_UUID")
    }

    @Test
    fun `should keep Spring's server error type when Open Producten fails`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(
                    502,
                ).setHeader("Content-Type", "application/json")
                .setBody("""{"detail": "Bad gateway"}"""),
        )

        val exception =
            assertThrows<HttpServerErrorException.BadGateway> {
                client.getProducttype(baseUrl, authentication, PRODUCTTYPE_UUID)
            }

        assertThat(exception.message).contains("producttype ophalen").contains("502").contains("Bad gateway")
    }

    @Test
    fun `should log and raise a readable error when Open Producten cannot be reached`() {
        val unreachable = baseUrl
        server.shutdown()

        val exception =
            assertThrows<ResourceAccessException> {
                client.getProducttypen(unreachable, authentication)
            }

        assertThat(exception.message).contains("producttypen ophalen").contains("geen verbinding")
        assertThat(logAppender.list.single().formattedMessage)
            .contains("GET $unreachable/producttypen/api/v1/producttypen")
            .contains("could not be reached")
    }

    private fun json(body: String) = MockResponse().setHeader("Content-Type", "application/json").setBody(body)

    companion object {
        private val PRODUCTTYPE_UUID = UUID.randomUUID().toString()
        private val PRODUCT_UUID = UUID.randomUUID().toString()
        private val OTHER_UUID = UUID.randomUUID().toString()
    }
}
