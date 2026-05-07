package com.github.jelatinone.fixtures;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public final class CanonicalTestFixtures {

	private CanonicalTestFixtures() {
	}

	public static URL url(String value) {
		try {
			return URI.create(value).toURL();
		} catch (MalformedURLException exception) {
			throw new IllegalArgumentException(exception);
		}
	}
}
