package com.github.scholarfind.api.store;

public interface Store<Body, Key> {

	void put(Body body);

	StoreItem get(Key key);

	void delete(Key key);
}
