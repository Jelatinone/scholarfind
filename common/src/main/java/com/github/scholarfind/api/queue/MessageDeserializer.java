package com.github.scholarfind.api.queue;

import lombok.NonNull;

@FunctionalInterface
public interface MessageDeserializer<T> {
	T decode(@NonNull QueueMessage body) throws Exception;
}
