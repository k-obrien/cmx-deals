package io

import io.ktor.client.HttpClient
import io.ktor.client.engine.curl.Curl
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.defaultRequest

private const val BASE_URL = "https://www.amazon.com/"

fun httpClient(): HttpClient = HttpClient(Curl) {
  expectSuccess = true
  BrowserUserAgent()
  defaultRequest { url(BASE_URL) }
}
