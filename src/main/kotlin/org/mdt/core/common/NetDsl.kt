package org.mdt.core.net

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSource
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ## Net
 *
 * Fluent, high-performance HTTP networking engine backed by OkHttp connection pooling.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
object Net {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

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

    fun get(url: String, block: RequestBuilder.() -> Unit = {}): HttpRequest = HttpRequest("GET", url, block)
    fun post(url: String, block: RequestBuilder.() -> Unit = {}): HttpRequest = HttpRequest("POST", url, block)
    fun put(url: String, block: RequestBuilder.() -> Unit = {}): HttpRequest = HttpRequest("PUT", url, block)
    fun delete(url: String, block: RequestBuilder.() -> Unit = {}): HttpRequest = HttpRequest("DELETE", url, block)
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

    fun header(name: String, value: String) { headers[name] = value }
    fun param(name: String, value: String) { queryParams[name] = value }

    fun json(body: String) {
        this.rawBody = body
        this.bodyMediaType = "application/json; charset=utf-8"
    }

    fun form(name: String, value: String) { formParams[name] = value }
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
        val response = Net.execute(request)
        if (!response.isSuccessful) throw IOException("HTTP ${response.code}: ${response.message}")
        return response.body?.string() ?: ""
    }

    /**
     * Executes request and returns response body as byte array.
     */
    suspend fun awaitBytes(): ByteArray {
        val request = buildOkHttpRequest()
        val response = Net.execute(request)
        if (!response.isSuccessful) throw IOException("HTTP ${response.code}: ${response.message}")
        return response.body?.bytes() ?: ByteArray(0)
    }

    /**
     * Executes request and returns Okio BufferedSource for streaming without loading into memory.
     */
    suspend fun awaitSource(): BufferedSource {
        val request = buildOkHttpRequest()
        val response = Net.execute(request)
        if (!response.isSuccessful) throw IOException("HTTP ${response.code}: ${response.message}")
        return response.body?.source() ?: throw IOException("Empty response body")
    }
}
