package com.github.jelatinone.meta.archetype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.jelatinone.meta.result.CollectionResult;
import com.github.jelatinone.meta.result.PostResult;

class ArchetypeContractsTest {

	@SuppressWarnings("resource")
	@Test
	void composition_delegates_collect_operate_and_post() {
		Composition<String, Integer> composition = new Composition<>(
				() -> new CollectionResult.Alive<>(List.of("alpha")),
				String::length,
				value -> value == 5 ? new PostResult.Success() : new PostResult.Fatal(new IllegalStateException("bad")));

		assertInstanceOf(CollectionResult.Alive.class, composition.collect());
		assertEquals(5, composition.operate("alpha"));
		assertInstanceOf(PostResult.Success.class, composition.post(5));
	}
}
