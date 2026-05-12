package com.github.jelatinone.mock;

import java.util.Collections;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.jelatinone.api.Criteria;
import com.github.jelatinone.api.Query.Count;
import com.github.jelatinone.api.Query.Exists;
import com.github.jelatinone.api.Query.Several;
import com.github.jelatinone.api.Query.Singular;
import com.github.jelatinone.api.store.Store;

public class MockStore<Key, Value> implements Store<Key, Value, Criteria<Key>> {

  private final Map<Key, Value> values;
  private boolean closed;

  public MockStore() {
    this(Map.of());
  }

  public MockStore(Map<Key, Value> initialValues) {
    this.values = new HashMap<>(initialValues);
  }

  @Override
  public synchronized void put(Key key, Value body) {
    values.put(key, body);
  }

  @Override
  public synchronized boolean query(Exists<Criteria<Key>> query) {
    return query(new Count<>(query.criteria())) > 0;
  }

  @Override
  public synchronized long query(Count<Criteria<Key>> query) {
    return values.containsKey(identifier(query.criteria())) ? 1L : 0L;
  }

  @Override
  public synchronized Optional<Value> query(Singular<Criteria<Key>> query) {
    return Optional.ofNullable(values.get(identifier(query.criteria())));
  }

  @Override
  public synchronized Collection<Value> query(Several<Criteria<Key>> query) {
    return query(new Singular<>(query.criteria()))
        .map(List::of)
        .orElseGet(List::of);
  }

  public synchronized Value get(Key key) {
    return values.get(key);
  }

  public synchronized void delete(Key key) {
    values.remove(key);
  }

  @Override
  public synchronized void delete(Singular<Criteria<Key>> query) {
    delete(identifier(query.criteria()));
  }

  @Override
  public synchronized void delete(Several<Criteria<Key>> query) {
    delete(identifier(query.criteria()));
  }

  public synchronized boolean containsKey(Key key) {
    return values.containsKey(key);
  }

  public synchronized int size() {
    return values.size();
  }

  public synchronized Map<Key, Value> snapshot() {
    return Collections.unmodifiableMap(new HashMap<>(values));
  }

  public synchronized boolean isClosed() {
    return closed;
  }

  @Override
  public synchronized void close() {
    closed = true;
  }

  private Key identifier(Criteria<Key> criteria) {
    return criteria.identifier().orElseThrow();
  }
}
