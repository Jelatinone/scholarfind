package com.github.jelatinone.infra.aws.jackson;

import com.fasterxml.jackson.databind.JavaType;
import com.github.jelatinone.infra.JacksonMapper;
import com.github.jelatinone.infra.aws.serial.SqsSerializer;

import java.util.Map;

public final class JacksonSqsSerializer<Value> implements SqsSerializer<Value> {
	private final JavaType type;

	public JacksonSqsSerializer(Class<Value> type) {
		this.type = JacksonMapper.mapper.getTypeFactory().constructType(type);
	}

	public JacksonSqsSerializer(JavaType type) {
		this.type = type;
	}

	@Override
	public Value decode(String body, Map<String, String> attributes) throws Exception {
		return JacksonMapper.mapper.readValue(body, type);
	}

	@Override
	public String encodeBody(Value value) throws Exception {
		return JacksonMapper.mapper.writeValueAsString(value);
	}

	@Override
	public Map<String, String> encodeAttributes(Value value) {
		return Map.of();
	}
}
