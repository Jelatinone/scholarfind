package com.github.scholarfind.utility;

@FunctionalInterface
public interface Factory<Create, Resource> {

  Create create(Resource logger);
}
