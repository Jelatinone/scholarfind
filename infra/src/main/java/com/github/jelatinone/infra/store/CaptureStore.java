package com.github.jelatinone.infra.store;

import com.github.jelatinone.infra.aws.store.S3Store;
import com.github.jelatinone.infra.aws.store.serial.JacksonS3Serializer;
import com.github.jelatinone.model.content.Capture;
import com.github.jelatinone.model.struct.Identity.TargetIdentity;

import software.amazon.awssdk.services.s3.S3Client;

public class CaptureStore extends S3Store<TargetIdentity, Capture> {

	public CaptureStore(S3Client client) {
		super(
				client,
				new JacksonS3Serializer<>(
						Capture.class,
						identity -> identity.identifier().toString()));
	}
}
