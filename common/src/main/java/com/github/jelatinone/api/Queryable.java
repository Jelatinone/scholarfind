package com.github.jelatinone.api;

import java.util.Collection;

public interface Queryable<Criterion, Result> {

  boolean query(Query.Exists<Criterion> query);

  long query(Query.Count<Criterion> query);

  Result query(Query.Singular<Criterion> query);

  Collection<Result> query(Query.Several<Criterion> query);
}