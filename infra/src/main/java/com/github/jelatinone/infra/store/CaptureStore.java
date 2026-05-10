package com.github.jelatinone.infra.store;

import java.util.UUID;

import com.github.jelatinone.infra.aws.store.S3Store;
import com.github.jelatinone.infra.aws.store.serial.JacksonS3Serializer;
import com.github.jelatinone.model.content.Capture;

import software.amazon.awssdk.services.s3.S3Client;

public class CaptureStore extends S3Store<UUID, Capture> {

	public CaptureStore(S3Client client) {
		super(
				client,
				new JacksonS3Serializer<>(
						Capture.class,
						UUID::toString));
	}
}
