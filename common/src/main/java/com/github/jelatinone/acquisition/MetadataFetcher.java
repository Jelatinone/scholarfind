package com.github.jelatinone.acquisition;

import java.net.URL;

import lombok.NonNull;

public interface MetadataFetcher {

	FetchedMetadata fetchMetadata(@NonNull URL canonicalUrl);
}
