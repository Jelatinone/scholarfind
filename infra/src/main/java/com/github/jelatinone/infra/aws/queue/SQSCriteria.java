package com.github.jelatinone.infra.aws.queue;

import java.util.Collection;
import java.util.Map;

import com.github.jelatinone.api.Criteria;

import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeNameForSends;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeValue;

public interface SQSCriteria extends Criteria<String> {

  String deduplicationId();

  String groupId();

  Collection<String> messageAttributeNames();

  Collection<MessageSystemAttributeName> messageSystemAttributeNames();

  Map<MessageSystemAttributeNameForSends, MessageSystemAttributeValue> messageSystemAttributes();
}
