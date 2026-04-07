package com.github.jelatinone.acquisition;

import java.net.URL;

import lombok.NonNull;

public interface MetadataFetcher {

  FetchedMetadata fetch(@NonNull URL url);
}
