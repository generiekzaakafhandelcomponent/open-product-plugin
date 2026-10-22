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

package com.ritense.openproduct.plugin

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ritense.openproduct.BaseIntegrationTest
import com.ritense.plugin.service.PluginService
import com.ritense.plugin.web.rest.request.PluginProcessLinkCreateDto
import com.ritense.processlink.domain.ActivityTypeWithEventName
import com.ritense.processlink.service.ProcessLinkService
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.operaton.bpm.engine.RepositoryService
import org.operaton.bpm.engine.RuntimeService
import org.operaton.bpm.engine.runtime.ProcessInstance
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class CreateOrUpdateProductIT : BaseIntegrationTest() {
    @Autowired
    lateinit var pluginService: PluginService

    @Autowired
    lateinit var processLinkService: ProcessLinkService

    @Autowired
    lateinit var repositoryService: RepositoryService

    @Autowired
    lateinit var runtimeService: RuntimeService

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private lateinit var server: MockWebServer
    private val responses = ConcurrentHashMap<String, MockResponse>()
    private lateinit var configurationId: UUID

    @BeforeEach
    fun setUp() {
        responses.clear()
        server = MockWebServer()
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse =
                    responses["${request.method} ${request.requestUrl!!.encodedPath}"]
                        ?: MockResponse().setResponseCode(404)
            }
        server.start()
        val token =
            pluginService.createPluginConfiguration(
                "Token",
                objectMapper.createObjectNode().put("token", "test-token"),
                "tokenauthentication",
            )
        configurationId =
            pluginService
                .createPluginConfiguration(
                    "Open Product",
                    objectMapper
                        .createObjectNode()
                        .put("baseUrl", server.url("/").toString().removeSuffix("/"))
                        .put("authenticationPluginConfiguration", token.id.id.toString()),
                    "openproduct",
                ).id.id
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `should create the product when the owner has none of the product type`() {
        givenProducts()
        givenSchema()
        responses["POST $PRODUCTEN_PATH"] =
            json(201, """{"uuid": "$NEW_UUID", "prijs": null, "frequentie": "", "status": "in_aanvraag"}""")

        val process =
            startCreateOrUpdate(
                mapOf(
                    "productNaam" to "pv:naam",
                    "productPrijs" to "12.50",
                    "gepubliceerd" to "false",
                    "aanvraagZaakUrn" to "pv:zaakUrn",
                    "aanvraagZaakUrl" to "https://zaken.example/zaken/1",
                    "documenten" to "pv:zaakDocumenten",
                    "dataobjectMapping" to DATAOBJECT_MAPPING,
                ),
                mapOf(
                    "naam" to "Parkeervergunning AB-12-CD",
                    "zaakUrn" to "urn:nld:gemeente:zaak:1",
                    "zaakDocumenten" to listOf(mapOf("url" to "https://documenten.example/1")),
                    "kenteken" to "AB-12-CD",
                    "actief" to "true",
                ),
            )

        val requests = takeRequests()
        assertThat(requests.map { "${it.method} ${it.requestUrl!!.encodedPath}" })
            .containsExactly("GET $PRODUCTTYPEN_PATH/$PRODUCTTYPE_UUID", "GET $PRODUCTEN_PATH", "POST $PRODUCTEN_PATH")
        assertThat(requests[1].requestUrl!!.queryParameter("producttype__uuid")).isEqualTo(PRODUCTTYPE_UUID)
        assertThat(requests[1].requestUrl!!.queryParameter("eigenaren__bsn")).isEqualTo(BSN)
        val body = body(requests[2])
        assertThat(body["producttype_uuid"].asText()).isEqualTo(PRODUCTTYPE_UUID)
        assertThat(body["eigenaren"].toString()).isEqualTo("""[{"bsn":"$BSN"}]""")
        assertThat(body["naam"].asText()).isEqualTo("Parkeervergunning AB-12-CD")
        assertThat(body["prijs"].asText()).isEqualTo("12.50")
        assertThat(body["gepubliceerd"].booleanValue()).isFalse()
        assertThat(body["aanvraag_zaak_urn"].asText()).isEqualTo("urn:nld:gemeente:zaak:1")
        assertThat(body["aanvraag_zaak_url"].asText()).isEqualTo("https://zaken.example/zaken/1")
        assertThat(body["documenten"].toString()).isEqualTo("""[{"url":"https://documenten.example/1"}]""")
        assertThat(body["status"].asText()).isEqualTo("initieel")
        assertThat(body.has("frequentie")).isFalse()
        assertThat(body["dataobject"].toString())
            .isEqualTo(
                """{"kenteken":"AB-12-CD","jaar":"2026","aantalUren":12,"actief":true,"opmerking":"Aanvraag: 2026"}""",
            )
        assertThat(runtimeService.getVariable(process.id, "productUuid")).isEqualTo(NEW_UUID)
    }

    @Test
    fun `should send object values as they are and a value that does not fit the schema type unchanged`() {
        givenProducts()
        givenSchema()
        responses["POST $PRODUCTEN_PATH"] = json(201, """{"uuid": "$NEW_UUID"}""")

        startCreateOrUpdate(
            mapOf(
                "dataobjectMapping" to
                    listOf(
                        mapOf("key" to "adres", "value" to "pv:adres"),
                        mapOf("key" to "aantalUren", "value" to "veel"),
                        mapOf("key" to "code", "value" to "12"),
                        mapOf("key" to "opmerking", "value" to "https://example.org/kaart?id=42"),
                    ),
            ),
            mapOf("adres" to mapOf("straat" to "Kerkstraat", "huisnummer" to 1)),
        )

        val body = body(takeRequests().last())
        assertThat(
            body["dataobject"].toString(),
        ).isEqualTo(
            """{"adres":{"straat":"Kerkstraat","huisnummer":1},"aantalUren":"veel","code":"12","opmerking":"https://example.org/kaart?id=42"}""",
        )
    }

    @Test
    fun `should create a new product when the owner only has ended products of the type`() {
        givenProducts(product(EXISTING_UUID, "ingetrokken"), product(OTHER_UUID, "verlopen"))
        responses["POST $PRODUCTEN_PATH"] = json(201, """{"uuid": "$NEW_UUID", "status": "initieel"}""")

        val process = startCreateOrUpdate(mapOf("resultaatVariabelNaam" to "opgeslagenProduct"))

        assertThat(takeRequests().map { it.method }).containsExactly("GET", "POST")
        assertThat(runtimeService.getVariable(process.id, "opgeslagenProduct")).isEqualTo(NEW_UUID)
    }

    @Test
    fun `should update the owner's current product, keeping owners, request, data and documents it was not given`() {
        givenProducts(
            product(OTHER_UUID, "verlopen"),
            """{"uuid": "$EXISTING_UUID", "status": "actief",
               "dataobject": {"kenteken": "OLD-01", "bestaand": "blijft"},
               "documenten": [{"url": "https://documenten.example/1", "urn": null}, {"url": null, "urn": "urn:nld:documenten:1"}]}""",
        )
        givenSchema()
        responses["PATCH $PRODUCTEN_PATH/$EXISTING_UUID"] =
            json(200, """{"uuid": "$EXISTING_UUID", "status": "actief"}""")

        val process =
            startCreateOrUpdate(
                mapOf(
                    "productNaam" to "pv:naam",
                    "aanvraagZaakUrn" to "pv:zaakUrn",
                    "aanvraagZaakUrl" to "https://zaken.example/zaken/2",
                    "documenten" to "pv:zaakDocumenten",
                    "dataobjectMapping" to listOf(mapOf("key" to "kenteken", "value" to "pv:kenteken")),
                ),
                mapOf(
                    "naam" to "Parkeervergunning XY-99-ZZ",
                    "zaakUrn" to "urn:nld:gemeente:zaak:2",
                    "kenteken" to "XY-99-ZZ",
                    "zaakDocumenten" to
                        listOf(
                            mapOf("url" to "https://documenten.example/1"),
                            mapOf(
                                "url" to "https://documenten.example/2",
                            ),
                        ),
                ),
            )

        val requests = takeRequests()
        assertThat(requests.map { it.method }).containsExactly("GET", "GET", "PATCH")
        val body = body(requests[2])
        assertThat(
            body.fieldNames().asSequence().toList(),
        ).containsExactlyInAnyOrder("naam", "dataobject", "documenten")
        assertThat(body["naam"].asText()).isEqualTo("Parkeervergunning XY-99-ZZ")
        assertThat(body["dataobject"].toString()).isEqualTo("""{"kenteken":"XY-99-ZZ","bestaand":"blijft"}""")
        assertThat(body["documenten"].toString())
            .isEqualTo(
                """[{"url":"https://documenten.example/1"},{"urn":"urn:nld:documenten:1"},{"url":"https://documenten.example/2"}]""",
            )
        assertThat(runtimeService.getVariable(process.id, "productUuid")).isEqualTo(EXISTING_UUID)
    }

    @Test
    fun `should look the owner up by KVK number and vestigingsnummer`() {
        givenProducts()
        responses["POST $PRODUCTEN_PATH"] = json(201, """{"uuid": "$NEW_UUID"}""")

        startCreateOrUpdate(
            mapOf(
                "eigenaarType" to "kvk_nummer",
                "eigenaarIdentificatie" to "12345678",
                "eigenaarVestigingsnummer" to "000012345678",
            ),
        )

        val requests = takeRequests()
        assertThat(requests[0].requestUrl!!.queryParameter("eigenaren__kvk_nummer")).isEqualTo("12345678")
        assertThat(requests[0].requestUrl!!.queryParameter("eigenaren__vestigingsnummer")).isEqualTo("000012345678")
        assertThat(requests[0].requestUrl!!.queryParameter("eigenaren__bsn")).isNull()
        assertThat(body(requests[1])["eigenaren"].toString())
            .isEqualTo("""[{"kvk_nummer":"12345678","vestigingsnummer":"000012345678"}]""")
    }

    @Test
    fun `should fail with a clear message when the owner has more than one current product of the type`() {
        givenProducts(product(EXISTING_UUID, "actief"), product(OTHER_UUID, "in_aanvraag"))

        val exception = assertThrows<Exception> { startCreateOrUpdate(emptyMap()) }

        assertThat(messages(exception)).contains("2 lopende producten").contains(EXISTING_UUID).contains(OTHER_UUID)
        assertThat(takeRequests().map { it.method }).containsExactly("GET")
    }

    @Test
    fun `should fail before calling Open Producten when the owner is empty`() {
        val exception = assertThrows<Exception> { startCreateOrUpdate(emptyMap(), mapOf("bsn" to "")) }

        assertThat(messages(exception)).contains("de eigenaar (bsn) is leeg")
        assertThat(server.requestCount).isZero()
    }

    @Test
    fun `should fail before calling Open Producten when the product type is empty`() {
        val exception =
            assertThrows<Exception> {
                startCreateOrUpdate(mapOf("producttypeUuid" to "pv:producttype"), mapOf("producttype" to ""))
            }

        assertThat(messages(exception)).contains("het producttype is leeg")
        assertThat(server.requestCount).isZero()
    }

    @Test
    fun `should fail with Open Producten's error when it rejects the product`() {
        givenProducts()
        responses["POST $PRODUCTEN_PATH"] =
            MockResponse()
                .setResponseCode(
                    400,
                ).setBody("""{"prijs": ["Zorg ervoor dat er niet meer dan 2 decimalen zijn."]}""")

        val exception = assertThrows<Exception> { startCreateOrUpdate(mapOf("productPrijs" to "1.234")) }

        assertThat(messages(exception))
            .contains("product aanmaken mislukt met status 400")
            .contains("Zorg ervoor dat er niet meer dan 2 decimalen zijn.")
    }

    @Test
    fun `should keep the existing create-product action working`() {
        responses["POST $PRODUCTEN_PATH"] =
            json(
                201,
                """{"uuid": "$NEW_UUID", "url": "http://open-product/producten/$NEW_UUID", "naam": "Vergunning",
                   "start_datum": null, "eind_datum": null, "aanmaak_datum": "2026-10-22T10:00:00Z",
                   "update_datum": "2026-10-22T10:00:00Z", "producttype": {"uuid": "$PRODUCTTYPE_UUID"},
                   "gepubliceerd": false, "eigenaren": [], "documenten": [], "status": "initieel",
                   "prijs": "10.00", "frequentie": "eenmalig", "verbruiksobject": null, "dataobject": null}""",
            )

        val process =
            startProcess(
                "create-product",
                mapOf(
                    "productNaam" to "Vergunning",
                    "productTypeUuid" to PRODUCTTYPE_UUID,
                    "eigenaarBsn" to BSN,
                    "aanvraagZaakUrn" to "pv:zaakUrn",
                    "aanvraagZaakUrl" to "https://zaken.example/zaken/1",
                    "productPrijs" to "10.00",
                    "productStatus" to "initieel",
                    "productFrequentie" to "eenmalig",
                ),
                mapOf("zaakUrn" to "urn:nld:gemeente:zaak:1"),
            )

        val body = body(takeRequests().single())
        assertThat(body["naam"].asText()).isEqualTo("Vergunning")
        assertThat(body["frequentie"].asText()).isEqualTo("eenmalig")
        assertThat(body["status"].asText()).isEqualTo("initieel")
        assertThat(body["eigenaren"].toString()).isEqualTo("""[{"bsn":"$BSN"}]""")
        assertThat(runtimeService.getVariable(process.id, "aangemaaktProductUuid")).isEqualTo(NEW_UUID)
    }

    private fun startCreateOrUpdate(
        actionProperties: Map<String, Any?>,
        variables: Map<String, Any?> = emptyMap(),
    ): ProcessInstance =
        startProcess(
            "create-or-update-product",
            mapOf(
                "producttypeUuid" to PRODUCTTYPE_UUID,
                "eigenaarType" to "bsn",
                "eigenaarIdentificatie" to "pv:bsn",
            ) + actionProperties,
            mapOf("bsn" to BSN) + variables,
        )

    private fun startProcess(
        actionKey: String,
        actionProperties: Map<String, Any?>,
        variables: Map<String, Any?>,
    ): ProcessInstance {
        val deployment =
            repositoryService
                .createDeployment()
                .addClasspathResource(
                    "bpmn/open-product-action-test.bpmn",
                ).deploy()
        val definition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.id).singleResult()
        processLinkService.getProcessLinks(definition.id).forEach { processLinkService.deleteProcessLink(it.id) }
        processLinkService.createProcessLink(
            PluginProcessLinkCreateDto(
                processDefinitionId = definition.id,
                activityId = "open-product-action",
                pluginConfigurationId = configurationId,
                pluginActionDefinitionKey = actionKey,
                actionProperties = objectMapper.valueToTree(actionProperties),
                activityType = ActivityTypeWithEventName.SERVICE_TASK_START,
            ),
            null,
        )
        return runtimeService.startProcessInstanceById(definition.id, UUID.randomUUID().toString(), variables)
    }

    private fun givenProducts(vararg products: String) {
        responses["GET $PRODUCTEN_PATH"] =
            json(200, """{"count": ${products.size}, "next": null, "results": [${products.joinToString()}]}""")
    }

    private fun givenSchema() {
        responses["GET $PRODUCTTYPEN_PATH/$PRODUCTTYPE_UUID"] =
            json(
                200,
                """{"uuid": "$PRODUCTTYPE_UUID", "naam": "Parkeervergunning", "dataobject_schema": {"naam": "parkeren", "schema": {
                   "type": "object", "required": ["kenteken"], "properties": {
                     "kenteken": {"type": "string"}, "jaar": {"type": "string"}, "aantalUren": {"type": "number"},
                     "actief": {"type": "boolean"}, "opmerking": {"type": "string"}, "adres": {"type": "object"},
                     "code": {"enum": ["12", "34"]}}}}}""",
            )
    }

    private fun product(
        uuid: String,
        status: String,
    ) = """{"uuid": "$uuid", "status": "$status"}"""

    private fun json(
        status: Int,
        body: String,
    ) = MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json").setBody(body)

    private fun takeRequests(): List<RecordedRequest> =
        generateSequence {
            server.takeRequest(100, TimeUnit.MILLISECONDS)
        }.toList()

    private fun body(request: RecordedRequest): JsonNode = objectMapper.readTree(request.body.readUtf8())

    private fun messages(exception: Throwable): String =
        generateSequence(exception) {
            it.cause
        }.joinToString(" | ") { it.message.orEmpty() }

    companion object {
        private const val PRODUCTEN_PATH = "/producten/api/v1/producten"
        private const val PRODUCTTYPEN_PATH = "/producttypen/api/v1/producttypen"
        private const val BSN = "999990019"
        private val PRODUCTTYPE_UUID = UUID.randomUUID().toString()
        private val NEW_UUID = UUID.randomUUID().toString()
        private val EXISTING_UUID = UUID.randomUUID().toString()
        private val OTHER_UUID = UUID.randomUUID().toString()
        private val DATAOBJECT_MAPPING =
            listOf(
                mapOf("key" to "kenteken", "value" to "pv:kenteken"),
                mapOf("key" to "jaar", "value" to "2026"),
                mapOf("key" to "aantalUren", "value" to "12"),
                mapOf("key" to "actief", "value" to "pv:actief"),
                mapOf("key" to "opmerking", "value" to "Aanvraag: 2026"),
                mapOf("key" to "adres", "value" to "pv:nietGezet"),
            )
    }
}
