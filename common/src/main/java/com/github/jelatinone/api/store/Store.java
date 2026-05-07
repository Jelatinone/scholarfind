package com.github.jelatinone.api.store;

public interface Store<Body, Key> extends AutoCloseable {

	void put(Key key, Body body);

	Body get(Key key);

	void delete(Key key);
}
