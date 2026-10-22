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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
data class PaginatedProducttypeList(
    @JsonProperty("next")
    val volgende: String?,
    @JsonProperty("results")
    val resultaten: List<ProducttypeDetails> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProducttypeDetails(
    @JsonProperty("uuid")
    val uuid: UUID,
    @JsonProperty("naam")
    val naam: String?,
    @JsonProperty("code")
    val code: String?,
    @JsonProperty("dataobject_schema")
    val dataobjectSchema: ProducttypeSchema?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProducttypeSchema(
    @JsonProperty("naam")
    val naam: String,
    @JsonProperty("schema")
    val schema: Map<String, Any?>,
)
