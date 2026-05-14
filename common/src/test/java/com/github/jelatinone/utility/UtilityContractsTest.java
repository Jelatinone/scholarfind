package com.github.jelatinone.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class UtilityContractsTest {

	@Test
	void canonicalizeUrl_normalizes_case_default_port_and_path() throws Exception {
		URL normalized = Canonical.canonicalizeURL("HTTPS://Example.com:443//path///?a=1");

		assertEquals("https://example.com/path?a=1", normalized.toExternalForm());
		assertEquals(Canonical.canonicalizeURL(normalized), normalized);
	}

	@Test
	void hash_and_target_use_stable_canonical_values() throws Exception {
		String hash = Canonical.hash("abc".getBytes());
		Instant discoveredAt = Instant.parse("2026-05-07T12:00:00Z");
		URL canonicalUrl = Canonical.canonicalizeURL("https://example.com/resource/");

		var target = Canonical.target(canonicalUrl, discoveredAt);

		assertEquals(
				HexFormat.of().formatHex(
						java.security.MessageDigest.getInstance("SHA-256").digest("abc".getBytes())),
				hash);
		assertEquals(1L, target.schemaVersion());
		assertEquals(Canonical.generateTargetUUID(canonicalUrl), target.targetId());
		assertEquals("https://example.com/resource", target.canonicalUrl().toExternalForm());
		assertEquals(discoveredAt, target.emittedAt());
	}

	@Test
	void canonical_parentEdge_uses_stable_directional_identity() throws Exception {
		Instant emittedAt = Instant.parse("2026-05-07T12:00:00Z");
		UUID reviewId = UUID.randomUUID();
		UUID parentId = UUID.randomUUID();
		var child = Canonical.target("https://example.com/child", emittedAt);

		var edge = Canonical.parent(reviewId, parentId, child, emittedAt);

		assertEquals(parentId, edge.from());
		assertEquals(child.targetId(), edge.to());
		assertEquals(edge.edgeId(), Canonical.parent(reviewId, parentId, child, emittedAt).edgeId());
	}

	@Test
	void locked_orders_by_remaining_delay() {
		Locked<String> faster = new Locked<>("fast", 1, TimeUnit.MILLISECONDS);
		Locked<String> slower = new Locked<>("slow", 10, TimeUnit.MILLISECONDS);

		assertTrue(faster.compareTo(slower) < 0);
		assertTrue(faster.getDelay(TimeUnit.MILLISECONDS) <= slower.getDelay(TimeUnit.MILLISECONDS));
	}

	@Test
	void mutable_and_factory_keep_supplied_value() {
		Mutable<String> mutable = new Mutable<>("alpha");
		Factory<Integer, String> factory = String::length;

		assertEquals("alpha", mutable.value);
		mutable.value = "beta";
		assertEquals("beta", mutable.value);
		assertEquals(4, factory.create("beta"));
	}

	@Test
	void canonicalConversions_roundTrip() throws Exception {
		URL url = URI.create("https://example.com/test").toURL();

		assertEquals(url, Canonical.toURI(url).toURL());
		assertEquals("/test", Canonical.canonicalizeURL("https://example.com/test/").getPath());
	}
}
