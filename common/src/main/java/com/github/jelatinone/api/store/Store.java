package com.github.jelatinone.api.store;

public interface Store<Body, Key> extends AutoCloseable {

  Key put(Body body);

  Body get(Key key);

  void delete(Key key);
}
