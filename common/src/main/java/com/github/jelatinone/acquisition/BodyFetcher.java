package com.github.jelatinone.acquisition;

import java.net.URL;

import lombok.NonNull;

public interface BodyFetcher {

  FetchedBody fetch(@NonNull URL url);
}