# Mock Server DSL
![MIT licensed](https://img.shields.io/badge/license-MIT-blue.svg)

A DSL to define [Dispatchers](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-dispatcher/) for OkHttp MockWebServer.

## Example
```kotlin
val server = MockWebServer()

val dispatcher = mockResponses {
    matchBody("foo") { MockResponse().setResponseCode(201) }
    matchPath("bar") { MockResponse().setResponseCode(200).setBody("baz") }
    matchMethod("GET") { MockResponse().setResponseCode(403) }
    matchMethod("POST") { MockResponse().setResponseCode(200) }
}

server.dispatcher = dispatcher
```

## License
mock-server-dsl is provided under the MIT license.
