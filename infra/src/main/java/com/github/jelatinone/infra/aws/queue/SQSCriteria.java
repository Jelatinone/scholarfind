package com.github.jelatinone.infra.aws.queue;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.jelatinone.api.Criteria;

import lombok.NonNull;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeNameForSends;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeValue;

public sealed interface SQSCriteria extends Criteria<String>
    permits SQSCriteria.Location, SQSCriteria.Send, SQSCriteria.Receive {

  static SQSCriteria location(String queueUrl) {
    return new Location(Optional.of(queueUrl), Optional.empty());
  }

  static SQSCriteria send(String queueUrl) {
    return new Send(
        Optional.of(queueUrl),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Map.of());
  }

  static SQSCriteria receive(String queueUrl) {
    return new Receive(
        Optional.of(queueUrl),
        Optional.empty(),
        List.of(),
        List.of());
  }

  default SQSCriteria withDuration(Duration duration) {
    return switch (this) {
      case Location location -> new Location(location.identifier(), Optional.of(duration));
      case Send send -> new Send(
          send.identifier(),
          Optional.of(duration),
          send.deduplicationId(),
          send.groupId(),
          send.messageSystemAttributes());
      case Receive receive -> new Receive(
          receive.identifier(),
          Optional.of(duration),
          receive.messageAttributeNames(),
          receive.messageSystemAttributeNames());
    };
  }

  default Send asSend() {
    return switch (this) {
      case Send send -> send;
      case Location location ->
        new Send(location.identifier(), location.duration(), Optional.empty(), Optional.empty(), Map.of());
      case Receive receive ->
        new Send(receive.identifier(), receive.duration(), Optional.empty(), Optional.empty(), Map.of());
    };
  }

  default Receive asReceive() {
    return switch (this) {
      case Receive receive -> receive;
      case Location location -> new Receive(location.identifier(), location.duration(), List.of(), List.of());
      case Send send -> new Receive(send.identifier(), send.duration(), List.of(), List.of());
    };
  }

  record Location(
      @NonNull Optional<String> identifier,
      @NonNull Optional<Duration> duration) implements SQSCriteria {
  }

  record Send(
      @NonNull Optional<String> identifier,
      @NonNull Optional<Duration> duration,
      @NonNull Optional<String> deduplicationId,
      @NonNull Optional<String> groupId,

      @NonNull Map<MessageSystemAttributeNameForSends, MessageSystemAttributeValue> messageSystemAttributes)
      implements SQSCriteria {
    public Send {
      messageSystemAttributes = Map.copyOf(messageSystemAttributes);
    }

    public Send withDeduplicationId(String deduplicationId) {
      return new Send(identifier(), duration(), Optional.ofNullable(deduplicationId), groupId,
          messageSystemAttributes());
    }

    public Send withGroupId(String groupId) {
      return new Send(identifier(), duration(), deduplicationId(), Optional.ofNullable(groupId),
          messageSystemAttributes());
    }

    public Send withMessageSystemAttributes(
        Map<MessageSystemAttributeNameForSends, MessageSystemAttributeValue> messageSystemAttributes) {
      return new Send(identifier(), duration(), deduplicationId(), groupId(), messageSystemAttributes());
    }
  }

  record Receive(
      @NonNull Optional<String> identifier,
      @NonNull Optional<Duration> duration,

      @NonNull Collection<String> messageAttributeNames,
      @NonNull Collection<MessageSystemAttributeName> messageSystemAttributeNames) implements SQSCriteria {
    public Receive {
      messageAttributeNames = List.copyOf(messageAttributeNames);
      messageSystemAttributeNames = List.copyOf(messageSystemAttributeNames);
    }

    public Receive withMessageAttributeNames(Collection<String> messageAttributeNames) {
      return new Receive(identifier, duration, messageAttributeNames, messageSystemAttributeNames);
    }

    public Receive withMessageSystemAttributeNames(Collection<MessageSystemAttributeName> messageSystemAttributeNames) {
      return new Receive(identifier, duration, messageAttributeNames, messageSystemAttributeNames);
    }
  }
}