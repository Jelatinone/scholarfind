package com.github.scholarfind.models;

import java.util.Map;

import com.github.scholarfind.utility.Builder;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

public interface Document<T extends Document<T>> {
  public Header header();

  public Timestamp timestamp();

  public Map<String, MessageAttributeValue> attribute();

  public Map<String, AttributeValue> itemize();

  public String json();

  public Builder<T> toBuilder();

}
