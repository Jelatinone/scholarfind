package com.github.jelatinone.acquisition.fetcher;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.github.jelatinone.acquisition.BodyFetcher;
import com.github.jelatinone.acquisition.FetchedBody;
import com.github.jelatinone.acquisition.FetchedMetadata;
import com.github.jelatinone.acquisition.MetadataFetcher;
import com.github.jelatinone.model.content.MediaEncoding;
import com.github.jelatinone.model.content.MediaMetadata;
import com.github.jelatinone.model.content.MediaType;
import com.github.jelatinone.utility.Canonical;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HttpFetcher implements BodyFetcher, MetadataFetcher {

	HttpClient client;

	Duration maximumRequestTimeout;
	int maximumRedirects;

	String userAgent;

	@Override
	public FetchedMetadata fetchMetadata(@NonNull URL canonicalUrl) {
		URI currentUri = Canonical.toURI(canonicalUrl);

		int redirectCount = 0;
		int setCookieCount = 0;

		try {
			while (true) {
				HttpRequest request = HttpRequest.newBuilder(currentUri)
						.timeout(maximumRequestTimeout)
						.header("User-Agent", userAgent)
						.method("HEAD", HttpRequest.BodyPublishers.noBody())
						.build();

				HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
				setCookieCount += response.headers().allValues("Set-Cookie").size();
				if (Canonical.isRedirect(response.statusCode()) && redirectCount < maximumRedirects) {
					String location = response.headers().firstValue("Location").orElse(null);
					if (location == null || location.isBlank()) {
						break;
					}
					currentUri = currentUri.resolve(location);
					redirectCount++;
					continue;
				}

				Long contentLength = response.headers().firstValue("Content-Length")
						.map((length) -> {
							try {
								return Long.parseLong(length);
							} catch (Exception exception) {
								return null;
							}
						})
						.orElse(null);
				String contentType = response.headers().firstValue("Content-Type").orElse(null);

				return new FetchedMetadata(
						Canonical.toURL(currentUri),
						MediaType.resolve(contentType),
						MediaEncoding.resolve(contentType),
						new MediaMetadata(
								response.statusCode(),
								redirectCount,
								setCookieCount,
								contentType,
								contentLength));
			}
			throw new IllegalStateException("Failed to resolve content exchange");
		} catch (IOException | InterruptedException exception) {
			throw new IllegalStateException("Failed to acquire content", exception);
		}
	}

	@Override
	public FetchedBody fetchBody(@NonNull URL canonicalUrl) {
		URI currentURI = Canonical.toURI(canonicalUrl);

		int redirectCount = 0;
		int setCookieCount = 0;

		try {
			while (true) {
				HttpRequest request = HttpRequest.newBuilder(currentURI)
						.timeout(maximumRequestTimeout)
						.header("User-Agent", userAgent)
						.GET()
						.build();

				HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
				setCookieCount += response.headers().allValues("Set-Cookie").size();
				if (Canonical.isRedirect(response.statusCode()) && redirectCount < maximumRedirects) {
					String location = response.headers().firstValue("Location").orElse(null);
					if (location == null || location.isBlank()) {
						break;
					}
					currentURI = currentURI.resolve(location);
					redirectCount++;
					continue;
				}

				Long contentLength = response.headers().firstValue("Content-Length")
						.map((length) -> {
							try {
								return Long.parseLong(length);
							} catch (Exception exception) {
								return null;
							}
						})
						.orElse((long) response.body().length);
				String contentType = response.headers().firstValue("Content-Type").orElse(null);

				return new FetchedBody(
						Canonical.toURL(currentURI),
						response.body(),
						Canonical.hash(response.body()),
						MediaType.resolve(contentType),
						MediaEncoding.resolve(contentType),
						new MediaMetadata(
								response.statusCode(),
								redirectCount,
								setCookieCount,
								contentType,
								contentLength));
			}
			throw new IllegalStateException("Failed to resolve content exchange");
		} catch (IOException | InterruptedException exception) {
			throw new IllegalStateException("Failed to acquire content", exception);
		}
	}

}
