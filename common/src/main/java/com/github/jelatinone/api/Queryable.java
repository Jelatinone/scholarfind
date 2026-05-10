package com.github.jelatinone.api;

import java.util.Collection;
import java.util.Optional;

public interface Queryable<Criterion, Result> {

  boolean query(Query.Exists<Criterion> query);

  long query(Query.Count<Criterion> query);

  Optional<Result> query(Query.Singular<Criterion> query);

  Collection<Result> query(Query.Several<Criterion> query);
}