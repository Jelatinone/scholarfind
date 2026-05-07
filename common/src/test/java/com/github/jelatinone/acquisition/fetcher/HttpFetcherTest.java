package com.github.jelatinone.acquisition.fetcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.jelatinone.acquisition.FetchedBody;
import com.github.jelatinone.acquisition.FetchedMetadata;
import com.github.jelatinone.model.content.MediaEncoding;
import com.github.jelatinone.model.content.MediaType;
import com.github.jelatinone.utility.Canonical;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

class HttpFetcherTest {

  private HttpServer server;

  @BeforeEach
  void startServer() throws Exception {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/redirect", this::handleRedirect);
    server.createContext("/final", this::handleFinal);
    server.start();
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  @Test
  void fetchMetadata_andFetchBody_followRedirectsAndDecodeHeaders() {
    HttpFetcher fetcher = new HttpFetcher(HttpClient.newHttpClient(), Duration.ofSeconds(2), 2, "JUnit");
    String start = "http://localhost:" + server.getAddress().getPort() + "/redirect";

    FetchedMetadata metadata = fetcher.fetchMetadata(Canonical.canonicalizeURL(start));
    FetchedBody body = fetcher.fetchBody(Canonical.canonicalizeURL(start));

    assertTrue(metadata.effectiveUrl().toExternalForm().endsWith("/final"));
    assertEquals(MediaType.TEXT_PLAIN, metadata.mediaType());
    assertEquals(MediaEncoding.UTF_8, metadata.mediaEncoding());
    assertEquals(1, metadata.mediaMetadata().redirectCount());
    assertEquals(1, metadata.mediaMetadata().cookieCount());

    assertEquals("hello", new String(body.sourceBytes()));
    assertEquals(Canonical.hash("hello".getBytes()), body.sourceHash());
    assertEquals(5L, body.mediaMetadata().contentLength());
  }

  private void handleRedirect(HttpExchange exchange) throws java.io.IOException {
    exchange.getResponseHeaders().add("Location", "/final");
    exchange.getResponseHeaders().add("Set-Cookie", "a=b");
    exchange.sendResponseHeaders(302, -1);
    exchange.close();
  }

  private void handleFinal(HttpExchange exchange) throws java.io.IOException {
    byte[] body = "hello".getBytes();
    exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
    exchange.getResponseHeaders().add("Content-Length", String.valueOf(body.length));
    if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
      exchange.sendResponseHeaders(200, -1);
    } else {
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
    }
    exchange.close();
  }
}
