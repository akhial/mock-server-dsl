// MIT License
//
// Copyright (c) 2023 Adel Khial
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package me.akhial.mockserverdsl

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class MockServerDslTest {

    @Test
    fun `mockResponses builds correct Dispatcher`() {
        val server = MockWebServer()
        val created = MockResponse().setResponseCode(201)
        val ok = MockResponse().setResponseCode(200)
        val notFound = MockResponse().setResponseCode(404)
        val forbidden = MockResponse().setResponseCode(403)

        server.enqueue(created)
        server.enqueue(ok)
        server.enqueue(notFound)
        server.enqueue(forbidden)

        assertEquals(201, post(server).code)
        assertEquals(200, post(server).code)
        assertEquals(404, get(server).code)
        assertEquals(403, put(server).code)


        val mockResponses = mockResponses {
            matchBody("body") { created }
            matchBody("body") { ok }
            matchPath("/api") { notFound }
            matchMethod("PUT") { forbidden }
        }
        server.dispatcher = mockResponses

        assertEquals(201, post(server).code)
        assertEquals(200, post(server).code)
        assertEquals(404, get(server).code)
        assertEquals(403, put(server).code)
    }

    private fun post(server: MockWebServer): Response {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(server.url("/"))
            .post("body".toRequestBody())
            .build()
        return client.newCall(request).execute()
    }

    private fun get(server: MockWebServer): Response {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(server.url("/api"))
            .get()
            .build()
        return client.newCall(request).execute()
    }

    private fun put(server: MockWebServer): Response {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(server.url("/"))
            .put("putting".toRequestBody())
            .build()
        return client.newCall(request).execute()
    }

}
