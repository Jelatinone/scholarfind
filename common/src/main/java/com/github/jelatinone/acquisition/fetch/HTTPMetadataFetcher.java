package com.github.jelatinone.acquisition.fetch;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.github.jelatinone.acquisition.FetchedMetadata;
import com.github.jelatinone.acquisition.MetadataFetcher;
import com.github.jelatinone.utility.Canonical;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HTTPMetadataFetcher implements MetadataFetcher {

  HttpClient client;
  Duration requestTimeout;
  int maxRedirects;
  String userAgent;

  @Override
  public FetchedMetadata fetch(@NonNull URL initialUrl) {
    URI currentURI = Canonical.toURI(initialUrl);
    int redirects = 0;
    int setCookieCount = 0;
    try {
      while (true) {
        HttpRequest request = HttpRequest.newBuilder(currentURI)
            .timeout(requestTimeout)
            .header("User-Agent", userAgent)
            .method("HEAD", HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        setCookieCount += response.headers().allValues("Set-Cookie").size();
        if (Canonical.isRedirect(response.statusCode()) && redirects < maxRedirects) {
          String location = response.headers().firstValue("Location").orElse(null);
          if (location == null || location.isBlank()) {
            break;
          }
          currentURI = currentURI.resolve(location);
          redirects++;
          continue;
        }

        Long contentLength = response.headers().firstValue("Content-Length")
            .map((length) -> {
              try {
                return (long) Long.parseLong(length);
              } catch (Exception exception) {
                return null;
              }
            })
            .orElse(null);
        return new FetchedMetadata(
            Canonical.toURL(currentURI),
            response.statusCode(),
            redirects,
            setCookieCount,
            response.headers().firstValue("Content-Type").orElse(null),
            contentLength);
      }
      throw new IllegalStateException("Failed to resolve content exchange");
    } catch (IOException | InterruptedException exception) {
      throw new IllegalStateException("Failed to acquire content", exception);
    }

  }
}
