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

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.util.*

private enum class MatchingStrategy { BODY, PATH, METHOD }

private data class MockResponseQueue(
    val identifier: String,
    val strategy: MatchingStrategy,
    val builders: Queue<() -> MockResponse> = LinkedList()
)

/**
 * DSL builder.
 */
class MockResponses {

    private val responseQueues = mutableListOf<MockResponseQueue>()

    /**
     * Adds a mock response to be matched by the request body.
     *
     * @param identifier The identifier to match in the request body.
     * @param builder A lambda that generates the mock response.
     */
    @Suppress("unused")
    fun matchBody(identifier: String, builder: () -> MockResponse) =
        appendBuilder(identifier, MatchingStrategy.BODY, builder)

    /**
     * Adds a mock response to be matched by the request path.
     *
     * @param identifier The identifier to match in the request path.
     * @param builder A lambda that generates the mock response.
     */
    @Suppress("unused")
    fun matchPath(identifier: String, builder: () -> MockResponse) =
        appendBuilder(identifier, MatchingStrategy.PATH, builder)

    /**
     * Adds a mock response to be matched by the request method.
     *
     * @param identifier The identifier to match in the request method.
     * @param builder A lambda that generates the mock response.
     */
    @Suppress("unused")
    fun matchMethod(identifier: String, builder: () -> MockResponse) =
        appendBuilder(identifier, MatchingStrategy.METHOD, builder)

    private fun appendBuilder(identifier: String, strategy: MatchingStrategy, builder: () -> MockResponse) {
        val queue = responseQueues.find { it.identifier == identifier && it.strategy == strategy }
        if (queue != null) {
            queue.builders.offer { builder() }
        } else {
            responseQueues.add(MockResponseQueue(identifier, strategy, LinkedList(listOf { builder() })))
        }
    }

    internal fun getResponse(request: RecordedRequest): MockResponse {
        val queue = responseQueues
            .filter {
                when (it.strategy) {
                    MatchingStrategy.BODY -> it.identifier in request.body.readUtf8()
                    MatchingStrategy.PATH -> it.identifier in request.path!!
                    MatchingStrategy.METHOD -> it.identifier in request.method!!
                }
            }
            .find { it.builders.isNotEmpty() }
            ?: error("Unexpected identifier: ${request.pretty()}")
        return queue.builders.poll()?.invoke() ?: error("Empty queue: ${request.pretty()}")
    }

    private fun RecordedRequest.pretty() = "${method}:${path} -> ${body.readUtf8()}"

}

/**
 * Creates a [Dispatcher] with the specified mock responses.
 *
 * Example:
 * ``` kotlin
 * mockResponses {
 *    matchBody("foo") { MockResponse().setResponseCode(201) }
 *    matchPath("bar") { MockResponse().setResponseCode(200).setBody("baz") }
 *    matchMethod("GET") { MockResponse().setResponseCode(403) }
 *    matchMethod("POST") { MockResponse().setResponseCode(200) }
 * }
 * ```
 *
 * The first matcher that matches the request according to any strategy will be used.
 *
 * @param init Mock responses configured via DSL
 * @return A [Dispatcher] instance with the mock responses configured.
 */
@Suppress("unused")
fun mockResponses(init: MockResponses.() -> Unit): Dispatcher {
    val responses = MockResponses()
    responses.init()
    return responses.toDispatcher()
}

private fun MockResponses.toDispatcher(): Dispatcher = object : Dispatcher() {

    override fun dispatch(request: RecordedRequest) = getResponse(request)

}
