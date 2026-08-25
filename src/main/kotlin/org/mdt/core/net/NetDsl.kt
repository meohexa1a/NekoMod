@file:Suppress("unused")

package org.mdt.core.net

import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSource
import java.io.IOException

/**
 * ## Net
 *
 * Fluent HTTP DSL for constructing and executing network requests asynchronously.
 * Fully compatible with runtime HJSON schemas and visual UI data binding.
 */
object Net {

    fun get(url: String, block: RequestBuilder.() -> Unit = {}): HttpRequest {
        return HttpRequest("GET", url, block)
    }

    fun post(url: String, block: RequestBuilder.() -> Unit = {}): HttpRequest {
        return HttpRequest("POST", url, block)
    }

    fun put(url: String, block: RequestBuilder.() -> Unit = {}): HttpRequest {
        return HttpRequest("PUT", url, block)
    }

    fun delete(url: String, block: RequestBuilder.() -> Unit = {}): HttpRequest {
        return HttpRequest("DELETE", url, block)
    }
}

/**
 * Fluent request builder helper.
 */
class RequestBuilder {
    val headers = mutableMapOf<String, String>()
    val queryParams = mutableMapOf<String, String>()
    var rawBody: String? = null
    var bodyMediaType: String = "application/json; charset=utf-8"
    val formParams = mutableMapOf<String, String>()

    fun header(name: String, value: String) {
        headers[name] = value
    }

    fun param(name: String, value: String) {
        queryParams[name] = value
    }

    fun json(body: String) {
        this.rawBody = body
        this.bodyMediaType = "application/json; charset=utf-8"
    }

    fun form(name: String, value: String) {
        formParams[name] = value
    }
}

/**
 * Asynchronous HTTP request execution wrapper.
 */
class HttpRequest(
    private val method: String,
    private val rawUrl: String,
    private val block: RequestBuilder.() -> Unit
) {
    fun buildOkHttpRequest(): Request {
        val builder = RequestBuilder().apply(block)

        val urlBuilder = rawUrl.toHttpUrlOrNull()?.newBuilder()
            ?: throw IllegalArgumentException("Invalid HTTP URL: $rawUrl")

        for ((k, v) in builder.queryParams) {
            urlBuilder.addQueryParameter(k, v)
        }

        val requestBuilder = Request.Builder().url(urlBuilder.build())

        for ((k, v) in builder.headers) {
            requestBuilder.addHeader(k, v)
        }

        val requestBody = when {
            builder.rawBody != null -> builder.rawBody!!.toRequestBody(builder.bodyMediaType.toMediaType())
            builder.formParams.isNotEmpty() -> {
                val form = FormBody.Builder()
                for ((k, v) in builder.formParams) {
                    form.add(k, v)
                }
                form.build()
            }
            method in listOf("POST", "PUT", "PATCH") -> ByteArray(0).toRequestBody(null)
            else -> null
        }

        requestBuilder.method(method, requestBody)
        return requestBuilder.build()
    }

    /**
     * Executes request and returns response body as UTF-8 string.
     */
    suspend fun awaitString(): String {
        val request = buildOkHttpRequest()
        val response = HttpEngine.execute(request)
        if (!response.isSuccessful) {
            throw IOException("HTTP ${response.code}: ${response.message}")
        }
        return response.body?.string() ?: ""
    }

    /**
     * Executes request and returns response body as byte array.
     */
    suspend fun awaitBytes(): ByteArray {
        val request = buildOkHttpRequest()
        val response = HttpEngine.execute(request)
        if (!response.isSuccessful) {
            throw IOException("HTTP ${response.code}: ${response.message}")
        }
        return response.body?.bytes() ?: ByteArray(0)
    }

    /**
     * Executes request and returns Okio BufferedSource for streaming without loading into memory.
     */
    suspend fun awaitSource(): BufferedSource {
        val request = buildOkHttpRequest()
        val response = HttpEngine.execute(request)
        if (!response.isSuccessful) {
            throw IOException("HTTP ${response.code}: ${response.message}")
        }
        return response.body?.source() ?: throw IOException("Empty response body")
    }
}
