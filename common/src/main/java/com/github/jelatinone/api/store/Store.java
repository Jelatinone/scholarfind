package com.github.jelatinone.api.store;

import com.github.jelatinone.api.Criteria;
import com.github.jelatinone.api.Query;
import com.github.jelatinone.api.Queryable;

public interface Store<Body, Key> extends Queryable<Criteria<Key>, Body>, AutoCloseable {

  void put(Key key, Body body);

  void delete(Query.Singular<Criteria<Key>> query);

  void delete(Query.Several<Criteria<Key>> query);
}
