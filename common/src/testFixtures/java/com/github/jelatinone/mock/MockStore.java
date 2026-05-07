package com.github.jelatinone.mock;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.github.jelatinone.api.store.Store;

public class MockStore<Value, Key> implements Store<Value, Key> {

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
	public synchronized Value get(Key key) {
		return values.get(key);
	}

	@Override
	public synchronized void delete(Key key) {
		values.remove(key);
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
}
