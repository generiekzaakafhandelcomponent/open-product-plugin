package com.ritense.openproduct.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ritense.tokenauthentication.plugin.TokenAuthenticationPlugin
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class OpenProductClient {
    val urlPath = "/producten/api/v1/producten/"

    fun getProduct(
        baseUrl: String,
        authenticationPlugin: TokenAuthenticationPlugin,
        uuid: String,
    ): Product? {
        val restClient = getRestclient(baseUrl, authenticationPlugin)

        val response =
            restClient
                .get()
                .uri(urlPath + uuid)
                .retrieve()

        val result =
            response.toEntity(Product::class.java)
                ?: throw IllegalStateException("Failed to get product")

        return result.body
    }

    fun getAllProducts(
        baseUrl: String,
        authenticationPlugin: TokenAuthenticationPlugin,
        producttypeUuid: String? = null,
    ): List<ProductResponse>? {
        val restClient = getRestclient(baseUrl, authenticationPlugin)

        val uri = if (producttypeUuid != null) {
            "/producten/api/v1/producten?producttype__uuid=$producttypeUuid"
        } else {
            "/producten/api/v1/producten"
        }

        val response =
            restClient
                .get()
                .uri(uri)
                .retrieve()
                .toEntity(PaginatedProductList::class.java)

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

        val response =
            restClient
                .post()
                .uri("/producten/api/v1/producten")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestJson)
                .retrieve()

        val result =
            response.toEntity(Product::class.java)
                ?: throw IllegalStateException("Failed to create product")

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

        val response =
            restClient
                .patch()
                .uri(urlPath + request.uuid)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(requestJson)
                .retrieve()

        val result =
            response.toEntity(String::class.java)
                ?: throw IllegalStateException("Failed to update product")

        return result.body
    }

    fun deleteProduct(
        baseUrl: String,
        authenticationPlugin: TokenAuthenticationPlugin,
        uuid: String,
    ): String? {
        val restClient = getRestclient(baseUrl, authenticationPlugin)

        val response =
            restClient
                .delete()
                .uri(urlPath + uuid)
                .retrieve()

        val result =
            response.toEntity(String::class.java)
                ?: throw IllegalStateException("Failed to delete product")

        return result.body
    }

    fun getRestclient(
        baseUrl: String,
        authenticationPlugin: TokenAuthenticationPlugin,
    ): RestClient =
        authenticationPlugin
            .applyAuth(RestClient.builder())
            .baseUrl(baseUrl)
            .build()
}
