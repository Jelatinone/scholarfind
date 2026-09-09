package com.github.jelatinone.api;

import java.util.Collection;
import java.util.Optional;

import com.github.jelatinone.api.Query.Count;

public interface Queryable<Criterion, Result> {

	default Optional<Boolean> query(Query.Exists<Criterion> query) {
		return query(new Count<>(query.criteria())).map(value -> value > 0);
	}

	Optional<Long> query(Query.Count<Criterion> query);

	Optional<Result> query(Query.Singular<Criterion> query);

	Collection<Result> query(Query.Several<Criterion> query);
}