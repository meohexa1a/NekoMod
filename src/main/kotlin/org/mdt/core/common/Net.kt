package org.mdt.core.common

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSource
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.collections.iterator
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ## Net
 *
 * Fluent, high-performance HTTP networking engine backed by OkHttp connection pooling.
 * Supports cancellable coroutines, query parameters, custom headers, streaming sources, and JSON payloads.
 * Encapsulates all underlying network client dependencies to prevent platform data type leakage.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
object Net {

    /** Shared OkHttpClient instance configured with connection pooling and timeouts. */
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // =========================================================================
    // I. Fluent DSL Entry Points
    // =========================================================================

    /** Initiates a GET request builder. */
    fun get(url: String, block: RequestBuilder.() -> Unit = {}): HttpRequest = HttpRequest("GET", url, block)

    /** Initiates a POST request builder. */
    fun post(url: String, block: RequestBuilder.() -> Unit = {}): HttpRequest = HttpRequest("POST", url, block)

    /** Initiates a PUT request builder. */
    fun put(url: String, block: RequestBuilder.() -> Unit = {}): HttpRequest = HttpRequest("PUT", url, block)

    /** Initiates a DELETE request builder. */
    fun delete(url: String, block: RequestBuilder.() -> Unit = {}): HttpRequest = HttpRequest("DELETE", url, block)

    // =========================================================================
    // II. Asynchronous Request Execution
    // =========================================================================

    /**
     * Executes an [okhttp3.Request] as an asynchronous, cancellable coroutine.
     *
     * @param request The prepared OkHttp request.
     * @return The received HTTP [Response].
     */
    suspend fun execute(request: Request): Response = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (continuation.isActive) continuation.resume(response)
            }
        })
    }
}

/**
 * ## RequestBuilder
 *
 * Fluent configuration builder for HTTP headers, query parameters, form fields, and raw JSON payloads.
 */
class RequestBuilder {
    val headers = mutableMapOf<String, String>()
    val queryParams = mutableMapOf<String, String>()
    var rawBody: String? = null
    var bodyMediaType: String = "application/json; charset=utf-8"
    val formParams = mutableMapOf<String, String>()

    /** Adds an HTTP header key-value pair. */
    fun header(name: String, value: String) { headers[name] = value }

    /** Adds an HTTP query parameter. */
    fun param(name: String, value: String) { queryParams[name] = value }

    /** Sets the raw JSON body payload. */
    fun json(body: String) {
        this.rawBody = body
        this.bodyMediaType = "application/json; charset=utf-8"
    }

    /** Adds a form field parameter. */
    fun form(name: String, value: String) { formParams[name] = value }
}

/**
 * ## HttpRequest
 *
 * Asynchronous HTTP request execution wrapper providing string, byte array, and streaming source readers.
 */
class HttpRequest(
    private val method: String,
    private val rawUrl: String,
    private val block: RequestBuilder.() -> Unit
) {
    // =========================================================================
    // I. Asynchronous Response Readers
    // =========================================================================

    /**
     * Executes the request and decodes the response body as a UTF-8 string.
     */
    suspend fun awaitString(): String {
        val request = buildOkHttpRequest()
        val response = Net.execute(request)
        if (!response.isSuccessful) throw IOException("HTTP ${response.code}: ${response.message}")
        return response.body?.string() ?: ""
    }

    /**
     * Executes the request and returns the response body as raw bytes.
     */
    suspend fun awaitBytes(): ByteArray {
        val request = buildOkHttpRequest()
        val response = Net.execute(request)
        if (!response.isSuccessful) throw IOException("HTTP ${response.code}: ${response.message}")
        return response.body?.bytes() ?: ByteArray(0)
    }

    /**
     * Executes the request and returns an Okio [BufferedSource] for streaming without loading into RAM.
     */
    suspend fun awaitSource(): BufferedSource {
        val request = buildOkHttpRequest()
        val response = Net.execute(request)
        if (!response.isSuccessful) throw IOException("HTTP ${response.code}: ${response.message}")
        return response.body?.source() ?: throw IOException("Empty response body")
    }

    // =========================================================================
    // II. Internal Request Assembly
    // =========================================================================

    /**
     * Builds the underlying [okhttp3.Request] instance.
     */
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
}
