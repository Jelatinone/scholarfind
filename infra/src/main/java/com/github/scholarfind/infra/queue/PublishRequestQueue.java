package com.github.scholarfind.infra.queue;



import com.github.scholarfind.infra.aws.SqsQueue;
import com.github.scholarfind.infra.aws.serial.JacksonSqsSerializer;
import com.github.scholarfind.models.publish.PublishRequest;

import software.amazon.awssdk.services.sqs.SqsClient;

public final class PublishRequestQueue extends SqsQueue<PublishRequest> {
  public PublishRequestQueue(
      SqsClient client,
      String queueUrl) {
    super(client, queueUrl, null, null, new JacksonSqsSerializer<>(PublishRequest.class));
  }
}
