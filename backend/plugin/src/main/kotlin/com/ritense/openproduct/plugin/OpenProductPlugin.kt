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

import com.ritense.openproduct.client.DataBindingConfig
import com.ritense.openproduct.client.DocumentRequest
import com.ritense.openproduct.client.EigenaarRequest
import com.ritense.openproduct.client.FrequentieEnum
import com.ritense.openproduct.client.OpenProductClient
import com.ritense.openproduct.client.ProductDetails
import com.ritense.openproduct.client.ProductRequest
import com.ritense.openproduct.client.ProducttypeDetails
import com.ritense.openproduct.client.ProducttypeSchema
import com.ritense.openproduct.client.StatusEnum
import com.ritense.plugin.annotation.Plugin
import com.ritense.plugin.annotation.PluginAction
import com.ritense.plugin.annotation.PluginActionProperty
import com.ritense.plugin.annotation.PluginProperty
import com.ritense.plugin.service.PluginService
import com.ritense.processlink.domain.ActivityTypeWithEventName
import com.ritense.tokenauthentication.plugin.TokenAuthenticationPlugin
import com.ritense.valueresolver.ValueResolverService
import com.ritense.zakenapi.ZakenApiPlugin
import io.github.oshai.kotlinlogging.KotlinLogging
import org.operaton.bpm.engine.delegate.DelegateExecution
import java.math.BigInteger
import java.net.URI
import java.util.UUID

@Plugin(
    key = "openproduct",
    title = "Open Product",
    description = "Plugin for interacting with the Open Product API",
)
class OpenProductPlugin(
    private val pluginService: PluginService,
    private val openProductClient: OpenProductClient,
    private val valueResolverService: ValueResolverService,
) {
    @PluginProperty(key = "baseUrl", secret = false, required = true)
    lateinit var baseUrl: String

    @PluginProperty(key = "authenticationPluginConfiguration", secret = false)
    lateinit var authenticationPluginConfiguration: TokenAuthenticationPlugin

    @PluginAction(
        key = "get-product",
        title = "Retrieve product plugin action",
        description = "Retrieve product via UUID plugin action",
        activityTypes = [ActivityTypeWithEventName.SERVICE_TASK_START],
    )
    fun getProduct(
        execution: DelegateExecution,
        @PluginActionProperty productUuid: String,
        @PluginActionProperty dataobjectVariabelNaam: String?,
    ) {
        val result =
            openProductClient.getProduct(
                baseUrl,
                authenticationPluginConfiguration,
                productUuid,
            )

        dataobjectVariabelNaam?.let {
            execution.setVariable(it, LinkedHashMap(result?.dataobject ?: emptyMap()))
        }
        execution.setVariable("resultaatPV", "Product: $result")
    }

    @PluginAction(
        key = "get-zaak-documenten",
        title = "Zaakinformatieobjecten ophalen als productdocumenten",
        description = "Retrieve informatieobjecten linked to the zaak, formatted for product 'documenten'",
        activityTypes = [ActivityTypeWithEventName.SERVICE_TASK_START],
    )
    fun getZaakDocumenten(
        execution: DelegateExecution,
        @PluginActionProperty aanvraagZaakUrl: String,
        @PluginActionProperty zaakDocumentenVariabelNaam: String?,
    ) {
        val documenten = fetchZaakDocumentUrls(execution, aanvraagZaakUrl).map { mapOf("url" to it.url) }
        execution.setVariable(zaakDocumentenVariabelNaam ?: "zaakDocumenten", documenten)
        execution.setVariable("resultaatPV", "Documenten opgehaald: ${documenten.size}")
    }

    @PluginAction(
        key = "get-all-products",
        title = "Retrieve all products plugin action",
        description = "Retrieve all products plugin action",
        activityTypes = [ActivityTypeWithEventName.SERVICE_TASK_START],
    )
    fun getAllProducts(
        execution: DelegateExecution,
        @PluginActionProperty producttypeUuid: String?,
        @PluginActionProperty eigenaarBsn: String?,
        @PluginActionProperty status: String?,
        @PluginActionProperty resultaatVariabelNaam: String?,
    ) {
        val result =
            openProductClient.getAllProducts(
                baseUrl,
                authenticationPluginConfiguration,
                producttypeUuid,
                eigenaarBsn,
                status,
            )

        val variabelNaam = resultaatVariabelNaam ?: "alleProducten"
        execution.setVariable(variabelNaam, result)
        execution.setVariable(
            "${variabelNaam}Tekst",
            result?.joinToString("\n") { "- ${it.naam ?: "(geen naam)"} | UUID: ${it.uuid} | Status: ${it.status}" }
                ?: "(geen producten gevonden)",
        )
        execution.setVariable("resultaatPV", "Producten opgehaald: ${result?.size ?: 0}")
    }

    @PluginAction(
        key = "create-product",
        title = "Create product plugin action",
        description = "Create product plugin action",
        activityTypes = [ActivityTypeWithEventName.SERVICE_TASK_START],
    )
    fun createProduct(
        execution: DelegateExecution,
        @PluginActionProperty productNaam: String,
        @PluginActionProperty productTypeUuid: String,
        @PluginActionProperty eigenaarBsn: String,
        @PluginActionProperty aanvraagZaakUrn: String,
        @PluginActionProperty aanvraagZaakUrl: String,
        @PluginActionProperty productPrijs: String,
        @PluginActionProperty productStatus: String,
        @PluginActionProperty productFrequentie: String,
        @PluginActionProperty gepubliceerd: java.lang.Boolean?,
        @PluginActionProperty dataobjectVariabelNaam: String?,
        @PluginActionProperty koppelZaakDocumenten: java.lang.Boolean?,
    ) {
        val freqEnum = toFreqEnum(productFrequentie)
        val statusEnum = toStatusEnum(productStatus)

        val dataobjectMap = resolveDataobjectMap(execution, dataobjectVariabelNaam)

        val documentenList =
            if (koppelZaakDocumenten == true) {
                fetchZaakDocumentUrls(execution, aanvraagZaakUrl)
            } else {
                null
            }

        val resultaat =
            openProductClient.createProduct(
                baseUrl,
                authenticationPluginConfiguration,
                ProductRequest(
                    naam = productNaam,
                    producttypeUuid = productTypeUuid,
                    eigenaren =
                        listOf(
                            EigenaarRequest(
                                bsn = eigenaarBsn,
                            ),
                        ),
                    gepubliceerd = gepubliceerd as Boolean?,
                    aanvraagZaakUrn = aanvraagZaakUrn,
                    aanvraagZaakUrl = aanvraagZaakUrl,
                    prijs = productPrijs,
                    frequentie = freqEnum,
                    status = statusEnum,
                    dataobject = dataobjectMap,
                    documenten = documentenList,
                ),
            )
        execution.setVariable("aangemaaktProductUuid", resultaat?.uuid?.toString())
        execution.setVariable("resultaatPV", "Product aangemaakt: $productNaam")
    }

    @PluginAction(
        key = "update-product",
        title = "Update product plugin action",
        description = "Update product plugin action",
        activityTypes = [ActivityTypeWithEventName.SERVICE_TASK_START],
    )
    fun updateProduct(
        execution: DelegateExecution,
        @PluginActionProperty productUuid: String,
        @PluginActionProperty productNaam: String,
        @PluginActionProperty productTypeUuid: String,
        @PluginActionProperty eigenaarBsn: String,
        @PluginActionProperty aanvraagZaakUrn: String?,
        @PluginActionProperty aanvraagZaakUrl: String?,
        @PluginActionProperty gepubliceerd: Boolean,
        @PluginActionProperty productPrijs: String,
        @PluginActionProperty productFrequentie: String,
        @PluginActionProperty productStatus: String,
        @PluginActionProperty dataobjectVariabelNaam: String?,
        @PluginActionProperty koppelZaakDocumenten: java.lang.Boolean?,
        @PluginActionProperty documentenVariabelNaam: String?,
    ) {
        val freqEnum = toFreqEnum(productFrequentie)
        val statusEnum = toStatusEnum(productStatus)

        val dataobjectMap = resolveDataobjectMap(execution, dataobjectVariabelNaam)

        val documentenList =
            when {
                documentenVariabelNaam != null -> resolveDocumentenList(execution, documentenVariabelNaam)
                koppelZaakDocumenten == true && aanvraagZaakUrl != null -> fetchZaakDocumentUrls(execution, aanvraagZaakUrl)
                else -> null
            }

        val resultaat =
            openProductClient.updateProduct(
                baseUrl,
                authenticationPluginConfiguration,
                ProductRequest(
                    uuid = productUuid,
                    naam = productNaam,
                    producttypeUuid = productTypeUuid,
                    eigenaren =
                        listOf(
                            EigenaarRequest(
                                bsn = eigenaarBsn,
                            ),
                        ),
                    gepubliceerd = gepubliceerd as Boolean?,
                    aanvraagZaakUrn = aanvraagZaakUrn,
                    aanvraagZaakUrl = aanvraagZaakUrl,
                    prijs = productPrijs,
                    frequentie = freqEnum,
                    status = statusEnum,
                    dataobject = dataobjectMap,
                    documenten = documentenList,
                ),
            )

        execution.setVariable("resultaatPV", "Product gewijzigd met UUID: $productUuid")
    }

    @PluginAction(
        key = "delete-product",
        title = "Delete product plugin action",
        description = "Delete product plugin action",
        activityTypes = [ActivityTypeWithEventName.SERVICE_TASK_START],
    )
    fun deleteProduct(
        execution: DelegateExecution,
        @PluginActionProperty productUuid: String,
    ) {
        val result =
            openProductClient.deleteProduct(
                baseUrl,
                authenticationPluginConfiguration,
                productUuid,
            )

        execution.setVariable("resultaatPV", "Product verwijderd met UUID: $productUuid")
    }

    @PluginAction(
        key = "create-or-update-product",
        title = "Create or update product plugin action",
        description = "Create the owner's product of a product type, or update it when the owner already has one",
        activityTypes = [ActivityTypeWithEventName.SERVICE_TASK_START],
    )
    fun createOrUpdateProduct(
        execution: DelegateExecution,
        @PluginActionProperty producttypeUuid: String,
        @PluginActionProperty eigenaarType: String,
        @PluginActionProperty eigenaarIdentificatie: String?,
        @PluginActionProperty eigenaarVestigingsnummer: String?,
        @PluginActionProperty productNaam: String?,
        @PluginActionProperty productStatus: String?,
        @PluginActionProperty productPrijs: String?,
        @PluginActionProperty productFrequentie: String?,
        @PluginActionProperty gepubliceerd: Boolean?,
        @PluginActionProperty startDatum: String?,
        @PluginActionProperty eindDatum: String?,
        @PluginActionProperty aanvraagZaakUrn: String?,
        @PluginActionProperty aanvraagZaakUrl: String?,
        @PluginActionProperty documenten: List<Map<String, Any?>>?,
        @PluginActionProperty dataobjectMapping: List<DataBindingConfig>?,
        @PluginActionProperty resultaatVariabelNaam: String?,
    ) {
        require(producttypeUuid.isNotBlank()) {
            "Open Product: het producttype is leeg, dus kan niet worden bepaald of het product al bestaat"
        }
        val eigenaar = toEigenaar(eigenaarType, eigenaarIdentificatie, eigenaarVestigingsnummer)
        val dataobject = resolveDataobjectMapping(execution, producttypeUuid, dataobjectMapping.orEmpty())
        val documentUrls = documenten?.mapNotNull { it["url"] as? String }
        val fields =
            mapOf(
                "naam" to productNaam,
                "status" to productStatus,
                "prijs" to productPrijs,
                "frequentie" to productFrequentie,
                "gepubliceerd" to gepubliceerd,
                "start_datum" to startDatum,
                "eind_datum" to eindDatum,
            ).filterValues { it != null && it.toString().isNotBlank() }

        val currentProducts =
            openProductClient
                .findProducten(baseUrl, authenticationPluginConfiguration, producttypeUuid, eigenaar)
                .filter { it.status !in ENDED_STATUSES }

        val product =
            when (currentProducts.size) {
                0 -> {
                    // Open Producten 1.7 fails with a server error on a create without status.
                    val createFields =
                        mapOf("status" to INITIAL_STATUS) +
                            fields +
                            mapOf(
                                "producttype_uuid" to producttypeUuid,
                                "eigenaren" to listOf(eigenaar),
                                "aanvraag_zaak_urn" to aanvraagZaakUrn,
                                "aanvraag_zaak_url" to aanvraagZaakUrl,
                                "dataobject" to dataobject.ifEmpty { null },
                                "documenten" to documentUrls?.distinct()?.map { mapOf("url" to it) },
                            ).filterValues { it != null && it.toString().isNotBlank() }
                    openProductClient.postProduct(baseUrl, authenticationPluginConfiguration, createFields).also {
                        logger.info { "Created product ${it.uuid} of product type $producttypeUuid" }
                    }
                }
                1 -> {
                    val existing = currentProducts.single()
                    val mergedDataobject = dataobject.ifEmpty { null }?.let { existing.dataobject.orEmpty() + it }
                    val updateFields =
                        fields +
                            listOfNotNull(
                                mergedDataobject?.let { "dataobject" to it },
                                documentUrls?.let { "documenten" to mergeDocumenten(existing, it) },
                            )
                    openProductClient
                        .patchProduct(baseUrl, authenticationPluginConfiguration, existing.uuid, updateFields)
                        .also { logger.info { "Updated product ${it.uuid} of product type $producttypeUuid" } }
                }
                else -> throw IllegalStateException(
                    "Open Product: de eigenaar heeft ${currentProducts.size} lopende producten van producttype " +
                        "$producttypeUuid (${currentProducts.joinToString { it.uuid.toString() }}); " +
                        "kies het product met de actie 'Product bijwerken via UUID'",
                )
            }

        execution.setVariable(resultaatVariabelNaam?.ifBlank { null } ?: "productUuid", product.uuid.toString())
        execution.setVariable("resultaatPV", "Product opgeslagen met UUID: ${product.uuid}")
    }

    fun getProducttypen(): List<ProducttypeDetails> =
        openProductClient.getProducttypen(baseUrl, authenticationPluginConfiguration)

    fun getDataobjectSchema(producttypeUuid: String): ProducttypeSchema? =
        openProductClient.getProducttype(baseUrl, authenticationPluginConfiguration, producttypeUuid).dataobjectSchema

    fun toFreqEnum(frequentie: String): FrequentieEnum =
        when (frequentie) {
            "eenmalig" -> FrequentieEnum.EENMALIG
            "jaarlijks" -> FrequentieEnum.JAARLIJKS
            "maandelijks" -> FrequentieEnum.MAANDELIJKS
            else -> throw IllegalArgumentException("Ongeldige frequentie: $frequentie")
        }

    fun toStatusEnum(status: String): StatusEnum =
        when (status) {
            "initieel" -> StatusEnum.INITIEEL
            "gereed" -> StatusEnum.GEREED
            "actief" -> StatusEnum.ACTIEF
            "ingetrokken" -> StatusEnum.INGETROKKEN
            "geweigerd" -> StatusEnum.GEWEIGERD
            "verlopen" -> StatusEnum.VERLOPEN
            else -> throw IllegalArgumentException("Ongeldige status: $status")
        }

    private fun fetchZaakDocumentUrls(execution: DelegateExecution, zaakUrl: String): List<DocumentRequest> {
        val zaakUri = URI(zaakUrl)
        val zakenApiPlugin = checkNotNull(
            pluginService.createInstance(ZakenApiPlugin::class.java, ZakenApiPlugin.findConfigurationByUrl(zaakUri))
        ) { "Could not find ZakenApiPlugin configuration for zaak with url: $zaakUri" }

        val documentId = UUID.fromString(execution.businessKey)
        return zakenApiPlugin.getZaakInformatieObjecten(documentId, zaakUri)
            .map { DocumentRequest(url = it.informatieobject.toString()) }
    }

    private fun resolveDataobjectMap(execution: DelegateExecution, variableName: String?): Map<String, Any>? =
        variableName?.let {
            val raw = execution.getVariable(it)
            if (raw != null && raw !is Map<*, *>) {
                logger.warn { "Expected Map for dataobject variable '$it' but got ${raw.javaClass}" }
            }
            @Suppress("UNCHECKED_CAST")
            raw as? Map<String, Any>
        }

    private fun resolveDocumentenList(execution: DelegateExecution, variableName: String?): List<DocumentRequest>? =
        variableName?.let {
            val raw = execution.getVariable(it)
            if (raw != null && raw !is List<*>) {
                logger.warn { "Expected List for documenten variable '$it' but got ${raw.javaClass}" }
            }
            @Suppress("UNCHECKED_CAST")
            (raw as? List<Map<String, Any>>)?.mapNotNull { entry ->
                (entry["url"] as? String)?.let { url -> DocumentRequest(url = url) }
            }
        }

    private fun toEigenaar(
        eigenaarType: String,
        identificatie: String?,
        vestigingsnummer: String?,
    ): EigenaarRequest {
        require(!identificatie.isNullOrBlank()) {
            "Open Product: de eigenaar ($eigenaarType) is leeg, dus kan niet worden bepaald of het product al bestaat"
        }
        return when (eigenaarType) {
            "bsn" -> EigenaarRequest(bsn = identificatie)
            "kvk_nummer" ->
                EigenaarRequest(
                    kvkNummer = identificatie,
                    vestigingsnummer = vestigingsnummer?.ifBlank { null },
                )
            "klantnummer" -> EigenaarRequest(klantnummer = identificatie)
            else -> throw IllegalArgumentException("Ongeldig eigenaartype: $eigenaarType")
        }
    }

    private fun resolveDataobjectMapping(
        execution: DelegateExecution,
        producttypeUuid: String,
        mapping: List<DataBindingConfig>,
    ): Map<String, Any> {
        val entries = mapping.filter { it.key.isNotBlank() && it.value.isNotBlank() }
        if (entries.isEmpty()) {
            return emptyMap()
        }
        // Only values with a known prefix are references; other text, such as 'Aanvraag: 2026', is a fixed value.
        val references = entries.map { it.value }.filter { isReference(it) }.toSet()
        val resolvedReferences = valueResolverService.resolveValues(execution.processInstanceId, execution, references)
        val properties =
            openProductClient
                .getProducttype(baseUrl, authenticationPluginConfiguration, producttypeUuid)
                .dataobjectSchema
                ?.schema
                ?.get("properties") as? Map<*, *>
        return entries
            .mapNotNull { entry ->
                val value = if (entry.value in references) resolvedReferences[entry.value] else entry.value
                value?.let { entry.key to toSchemaType(it, properties?.get(entry.key) as? Map<*, *>) }
            }.toMap()
    }

    // http(s) values are fixed values; Valtimo's resolver would cut them off at the '?'.
    private fun isReference(value: String): Boolean =
        value.contains(':') &&
            !value.startsWith("http:") &&
            !value.startsWith("https:") &&
            valueResolverService.supportsValue(value)

    private fun toSchemaType(
        value: Any,
        propertySchema: Map<*, *>?,
    ): Any {
        val types =
            when (val type = propertySchema?.get("type")) {
                is String -> listOf(type)
                is List<*> -> type.filterIsInstance<String>()
                else -> emptyList()
            }
        if (types.isEmpty() || value is Map<*, *> || value is Collection<*> || types.any { hasSchemaType(value, it) }) {
            return value
        }
        return types.firstNotNullOfOrNull { convertToSchemaType(value.toString(), it) } ?: value
    }

    private fun hasSchemaType(
        value: Any,
        type: String,
    ): Boolean =
        when (type) {
            "string" -> value is String
            "integer" -> value is Int || value is Long || value is Short || value is BigInteger
            "number" -> value is Number
            "boolean" -> value is Boolean
            else -> false
        }

    private fun convertToSchemaType(
        value: String,
        type: String,
    ): Any? =
        when (type) {
            "string" -> value
            "integer" -> value.trim().toBigIntegerOrNull()
            "number" -> value.trim().toBigDecimalOrNull()
            "boolean" -> value.trim().toBooleanStrictOrNull()
            else -> null
        }

    // Keeps every current document, URN-only ones included, and adds the configured URLs it does not have yet.
    private fun mergeDocumenten(
        existing: ProductDetails,
        urls: List<String>,
    ): List<Map<String, String>> {
        val current =
            existing.documenten.orEmpty().map { document ->
                listOfNotNull(document.url?.let { "url" to it }, document.urn?.let { "urn" to it }).toMap()
            }
        val currentUrls = current.mapNotNull { it["url"] }.toSet()
        return current + urls.distinct().filter { it !in currentUrls }.map { mapOf("url" to it) }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
        private val ENDED_STATUSES = setOf("ingetrokken", "geweigerd", "verlopen")
        private const val INITIAL_STATUS = "initieel"
    }
}
