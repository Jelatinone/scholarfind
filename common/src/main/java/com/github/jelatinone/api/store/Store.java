package com.github.jelatinone.api.store;

import com.github.jelatinone.api.Criteria;
import com.github.jelatinone.api.Query;
import com.github.jelatinone.api.Queryable;

public interface Store<Key, Value, Criterion extends Criteria<Key>> extends Queryable<Criterion, Value>, AutoCloseable {

  void put(Key key, Value body);

  void delete(Query.Singular<Criterion> query);

  void delete(Query.Several<Criterion> query);
}
