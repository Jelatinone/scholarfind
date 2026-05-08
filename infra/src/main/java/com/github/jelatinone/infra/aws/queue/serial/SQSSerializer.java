package com.github.jelatinone.infra.aws.queue.serial;

import java.io.IOException;
import java.util.Map;

public interface SQSSerializer<Value> {
	Value decode(String body, Map<String, String> attributes) throws IOException;

	String encodeBody(Value value) throws IOException;

	Map<String, String> encodeAttributes(Value value) throws IOException;
}
