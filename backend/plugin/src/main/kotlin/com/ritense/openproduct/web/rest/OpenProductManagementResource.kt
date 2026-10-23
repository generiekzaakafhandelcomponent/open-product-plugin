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

import com.ritense.openproduct.plugin.OpenProductPlugin
import com.ritense.openproduct.web.rest.dto.DataobjectSchemaDto
import com.ritense.openproduct.web.rest.dto.ProducttypeDto
import com.ritense.plugin.domain.PluginConfigurationId
import com.ritense.plugin.service.PluginService
import com.ritense.valtimo.contract.annotation.SkipComponentScan
import com.ritense.valtimo.contract.domain.ValtimoMediaType.APPLICATION_JSON_UTF8_VALUE
import com.ritense.valtimo.contract.endpoint.EndpointDescription
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@SkipComponentScan
@RequestMapping("/api", produces = [APPLICATION_JSON_UTF8_VALUE])
class OpenProductManagementResource(
    private val pluginService: PluginService,
) {
    @EndpointDescription(
        en = "List the product types of an Open Product plugin configuration",
        nl = "Producttypen van een Open Product-pluginconfiguratie ophalen",
    )
    @GetMapping("/management/v1/openproduct/{pluginConfigurationId}/producttypen")
    fun getProducttypen(
        @PathVariable pluginConfigurationId: UUID,
    ): ResponseEntity<List<ProducttypeDto>> {
        val producttypen = getPlugin(pluginConfigurationId).getProducttypen().map { ProducttypeDto.of(it) }
        return ResponseEntity.ok(producttypen)
    }

    @EndpointDescription(
        en = "Get the dataobject schema of a product type",
        nl = "Dataobject-schema van een producttype ophalen",
    )
    @GetMapping("/management/v1/openproduct/{pluginConfigurationId}/producttypen/{producttypeUuid}/dataobject-schema")
    fun getDataobjectSchema(
        @PathVariable pluginConfigurationId: UUID,
        @PathVariable producttypeUuid: UUID,
    ): ResponseEntity<DataobjectSchemaDto> {
        val schema = getPlugin(pluginConfigurationId).getDataobjectSchema(producttypeUuid.toString())
        return ResponseEntity.ok(DataobjectSchemaDto(schema = schema?.schema))
    }

    private fun getPlugin(pluginConfigurationId: UUID): OpenProductPlugin =
        pluginService.createInstance(PluginConfigurationId.existingId(pluginConfigurationId)) as? OpenProductPlugin
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Not an Open Product plugin configuration")
}
