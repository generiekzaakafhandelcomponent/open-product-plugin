package com.ritense.openproduct.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ritense.tokenauthentication.plugin.TokenAuthenticationPlugin
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.util.UriBuilder
import java.io.IOException
import java.net.URI
import java.util.UUID

@Component
class OpenProductClient {
    val urlPath = "/producten/api/v1/producten/"
    val producttypenPath = "/producttypen/api/v1/producttypen"

    fun getProduct(
        baseUrl: String,
        authenticationPlugin: TokenAuthenticationPlugin,
        uuid: String,
    ): Product? {
        val restClient = getRestclient(baseUrl, authenticationPlugin)

        val result =
            call("product ophalen", HttpMethod.GET, baseUrl, urlPath + uuid) {
                restClient
                    .get()
                    .uri(urlPath + uuid)
                    .retrieve()
                    .toEntity(Product::class.java)
            }

        return result.body
    }

    fun getAllProducts(
        baseUrl: String,
        authenticationPlugin: TokenAuthenticationPlugin,
        producttypeUuid: String? = null,
        eigenaarBsn: String? = null,
        status: String? = null,
    ): List<ProductResponse>? {
        val restClient = getRestclient(baseUrl, authenticationPlugin)

        val queryParams =
            listOfNotNull(
                producttypeUuid?.let { "producttype__uuid=$it" },
                eigenaarBsn?.let { "eigenaren__bsn=$it" },
                status?.let { "status=$it" },
            )
        val uri =
            if (queryParams.isNotEmpty()) {
                "${urlPath.dropLast(1)}?${queryParams.joinToString("&")}"
            } else {
                urlPath.dropLast(1)
            }

        val response =
            call("producten ophalen", HttpMethod.GET, baseUrl, uri) {
                restClient
                    .get()
                    .uri(uri)
                    .retrieve()
                    .toEntity(PaginatedProductList::class.java)
            }

        val result =
            response.body
                ?: throw IllegalStateException("Failed to get products")

        return result.resultaten
    }

    fun createProduct(
        baseUrl: String,
        authenticationPlugin: TokenAuthenticationPlugin,
        request: ProductRequest,
    ): Product? {
        val restClient = getRestclient(baseUrl, authenticationPlugin)
        val objectMapper = jacksonObjectMapper()
        val requestJson = objectMapper.writeValueAsString(request)

        val result =
            call("product aanmaken", HttpMethod.POST, baseUrl, urlPath.dropLast(1)) {
                restClient
                    .post()
                    .uri(urlPath.dropLast(1))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestJson)
                    .retrieve()
                    .toEntity(Product::class.java)
            }

        return result.body
    }

    fun updateProduct(
        baseUrl: String,
        authenticationPlugin: TokenAuthenticationPlugin,
        request: ProductRequest,
    ): String? {
        val restClient = getRestclient(baseUrl, authenticationPlugin)
        val objectMapper = jacksonObjectMapper()
        val requestJson = objectMapper.writeValueAsString(request)

        val result =
            call("product bijwerken", HttpMethod.PATCH, baseUrl, urlPath + request.uuid) {
                restClient
                    .patch()
                    .uri(urlPath + request.uuid)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(requestJson)
                    .retrieve()
                    .toEntity(String::class.java)
            }

        return result.body
    }

    fun deleteProduct(
        baseUrl: String,
        authenticationPlugin: TokenAuthenticationPlugin,
        uuid: String,
    ): String? {
        val restClient = getRestclient(baseUrl, authenticationPlugin)

        val result =
            call("product verwijderen", HttpMethod.DELETE, baseUrl, urlPath + uuid) {
                restClient
                    .delete()
                    .uri(urlPath + uuid)
                    .retrieve()
                    .toEntity(String::class.java)
            }

        return result.body
    }

    fun getProducttypen(
        baseUrl: String,
        authenticationPlugin: TokenAuthenticationPlugin,
    ): List<ProducttypeDetails> =
        getAllPages(
            "producttypen ophalen",
            baseUrl,
            authenticationPlugin,
            producttypenPath,
            emptyMap(),
            PaginatedProducttypeList::class.java,
            { it.volgende },
            { it.resultaten },
        )

    fun getProducttype(
        baseUrl: String,
        authenticationPlugin: TokenAuthenticationPlugin,
        uuid: String,
    ): ProducttypeDetails {
        val restClient = getRestclient(baseUrl, authenticationPlugin)
        val path = "$producttypenPath/$uuid"
        return call("producttype ophalen", HttpMethod.GET, baseUrl, path) {
            restClient
                .get()
                .uri(path)
                .retrieve()
                .body(ProducttypeDetails::class.java)
        } ?: throw IllegalStateException("Open Producten: producttype $uuid gaf een leeg antwoord")
    }

    fun findProducten(
        baseUrl: String,
        authenticationPlugin: TokenAuthenticationPlugin,
        producttypeUuid: String,
        eigenaar: EigenaarRequest,
    ): List<ProductDetails> {
        val filters =
            listOfNotNull(
                "producttype__uuid" to producttypeUuid,
                eigenaar.bsn?.let { "eigenaren__bsn" to it },
                eigenaar.kvkNummer?.let { "eigenaren__kvk_nummer" to it },
                eigenaar.vestigingsnummer?.let { "eigenaren__vestigingsnummer" to it },
                eigenaar.klantnummer?.let { "eigenaren__klantnummer" to it },
            ).toMap()
        return getAllPages(
            "producten zoeken",
            baseUrl,
            authenticationPlugin,
            urlPath.dropLast(1),
            filters,
            PaginatedProductDetailsList::class.java,
            { it.volgende },
            { it.resultaten },
        )
    }

    fun postProduct(
        baseUrl: String,
        authenticationPlugin: TokenAuthenticationPlugin,
        fields: Map<String, Any?>,
    ): ProductDetails {
        val restClient = getRestclient(baseUrl, authenticationPlugin)
        val path = urlPath.dropLast(1)
        return call("product aanmaken", HttpMethod.POST, baseUrl, path) {
            restClient
                .post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jacksonObjectMapper().writeValueAsString(fields))
                .retrieve()
                .body(ProductDetails::class.java)
        } ?: throw IllegalStateException("Open Producten: product aanmaken gaf een leeg antwoord")
    }

    fun patchProduct(
        baseUrl: String,
        authenticationPlugin: TokenAuthenticationPlugin,
        uuid: UUID,
        fields: Map<String, Any?>,
    ): ProductDetails {
        val restClient = getRestclient(baseUrl, authenticationPlugin)
        val path = urlPath + uuid
        return call("product bijwerken", HttpMethod.PATCH, baseUrl, path) {
            restClient
                .patch()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jacksonObjectMapper().writeValueAsString(fields))
                .retrieve()
                .body(ProductDetails::class.java)
        } ?: throw IllegalStateException("Open Producten: product $uuid bijwerken gaf een leeg antwoord")
    }

    fun getRestclient(
        baseUrl: String,
        authenticationPlugin: TokenAuthenticationPlugin,
    ): RestClient =
        authenticationPlugin
            .applyAuth(RestClient.builder())
            .baseUrl(baseUrl)
            .build()

    // Follows 'next' by its query only, so a base URL behind a proxy or with a path prefix keeps working.
    private fun <P, T> getAllPages(
        operation: String,
        baseUrl: String,
        authenticationPlugin: TokenAuthenticationPlugin,
        path: String,
        queryParams: Map<String, String>,
        pageType: Class<P>,
        next: (P) -> String?,
        items: (P) -> List<T>,
    ): List<T> {
        val restClient = getRestclient(baseUrl, authenticationPlugin)
        val results = mutableListOf<T>()
        var pageQuery: String? = null
        do {
            val uri: (UriBuilder) -> URI = { builder ->
                builder.path(path)
                if (pageQuery == null) {
                    queryParams.forEach { (name, value) -> builder.queryParam(name, value) }
                } else {
                    builder.query(pageQuery)
                }
                builder.build()
            }
            val page =
                call(operation, HttpMethod.GET, baseUrl, path) {
                    restClient
                        .get()
                        .uri(uri)
                        .retrieve()
                        .body(pageType)
                } ?: throw IllegalStateException("Open Producten: $operation gaf een leeg antwoord")
            results += items(page)
            pageQuery = next(page)?.let { URI(it).rawQuery }
        } while (pageQuery != null)
        return results
    }

    private fun <T> call(
        operation: String,
        method: HttpMethod,
        baseUrl: String,
        uri: String,
        block: () -> T,
    ): T =
        try {
            block()
        } catch (e: RestClientResponseException) {
            // The query is left out of the log: it holds the owner's BSN or KVK number.
            val path = uri.substringBefore('?')
            logger.error {
                "Open Producten $operation failed: $method $baseUrl$path returned ${e.statusCode.value()} " +
                    "with body: ${e.responseBodyAsString}"
            }
            throw withReadableMessage(operation, e)
        } catch (e: ResourceAccessException) {
            val path = uri.substringBefore('?')
            logger.error(e) { "Open Producten $operation failed: $method $baseUrl$path could not be reached" }
            throw ResourceAccessException(
                "Open Producten: $operation mislukt, geen verbinding: ${e.mostSpecificCause.message}",
                e.cause as? IOException,
            )
        }

    // Same exception subtype as Spring's, so existing catch clauses still match; only the message changes.
    private fun withReadableMessage(
        operation: String,
        e: RestClientResponseException,
    ): RestClientResponseException {
        val message =
            "Open Producten: $operation mislukt met status ${e.statusCode.value()}: " +
                e.responseBodyAsString.ifBlank { e.statusText }
        val readable =
            when (e) {
                is HttpClientErrorException ->
                    HttpClientErrorException.create(
                        message,
                        e.statusCode,
                        e.statusText,
                        e.responseHeaders ?: HttpHeaders(),
                        e.responseBodyAsByteArray,
                        null,
                    )
                is HttpServerErrorException ->
                    HttpServerErrorException.create(
                        message,
                        e.statusCode,
                        e.statusText,
                        e.responseHeaders ?: HttpHeaders(),
                        e.responseBodyAsByteArray,
                        null,
                    )
                else ->
                    RestClientResponseException(
                        message,
                        e.statusCode,
                        e.statusText,
                        e.responseHeaders,
                        e.responseBodyAsByteArray,
                        null,
                    )
            }
        return readable.apply { initCause(e) }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
