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

package com.ritense.openproduct.web.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.ritense.openproduct.BaseIntegrationTest
import com.ritense.plugin.service.PluginService
import com.ritense.valtimo.contract.authentication.AuthoritiesConstants.ADMIN
import com.ritense.valtimo.contract.authentication.AuthoritiesConstants.USER
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@AutoConfigureMockMvc
class OpenProductManagementResourceIT : BaseIntegrationTest() {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var pluginService: PluginService

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private lateinit var server: MockWebServer
    private lateinit var configurationId: UUID

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
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
    @WithMockUser(authorities = [ADMIN])
    fun `should list the product types of every result page for an admin`() {
        server.enqueue(
            json(
                """{"count": 2, "next": "http://open-product/producttypen/api/v1/producttypen?page=2",
                   "results": [{"uuid": "$PRODUCTTYPE_UUID", "naam": "Parkeervergunning", "code": "PV"}]}""",
            ),
        )
        server.enqueue(
            json("""{"count": 2, "next": null, "results": [{"uuid": "$OTHER_UUID", "naam": "Hondenbelasting"}]}"""),
        )

        mockMvc
            .perform(get("/api/management/v1/openproduct/$configurationId/producttypen"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].uuid").value(PRODUCTTYPE_UUID))
            .andExpect(jsonPath("$[0].naam").value("Parkeervergunning"))
            .andExpect(jsonPath("$[0].code").value("PV"))
            .andExpect(jsonPath("$[1].naam").value("Hondenbelasting"))
    }

    @Test
    @WithMockUser(authorities = [ADMIN])
    fun `should return the dataobject schema of a product type for an admin`() {
        server.enqueue(
            json(
                """{"uuid": "$PRODUCTTYPE_UUID", "naam": "Parkeervergunning", "dataobject_schema": {"naam": "parkeren",
                   "schema": {"type": "object", "required": ["kenteken"], "properties": {"kenteken": {"type": "string"}}}}}""",
            ),
        )

        mockMvc
            .perform(
                get("/api/management/v1/openproduct/$configurationId/producttypen/$PRODUCTTYPE_UUID/dataobject-schema"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.schema.required[0]").value("kenteken"))
            .andExpect(jsonPath("$.schema.properties.kenteken.type").value("string"))
    }

    @Test
    @WithMockUser(authorities = [USER])
    fun `should refuse the product types to a user who is not an admin`() {
        mockMvc
            .perform(get("/api/management/v1/openproduct/$configurationId/producttypen"))
            .andExpect(status().isForbidden)
        mockMvc
            .perform(
                get("/api/management/v1/openproduct/$configurationId/producttypen/$PRODUCTTYPE_UUID/dataobject-schema"),
            ).andExpect(status().isForbidden)
    }

    private fun json(body: String) = MockResponse().setHeader("Content-Type", "application/json").setBody(body)

    companion object {
        private val PRODUCTTYPE_UUID = UUID.randomUUID().toString()
        private val OTHER_UUID = UUID.randomUUID().toString()
    }
}
