package com.github.jelatinone.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.github.jelatinone.fixtures.StructTestFixtures;
import com.github.jelatinone.infra.aws.queue.serial.JacksonSQSSerializer;
import com.github.jelatinone.infra.aws.store.serial.JacksonDynamoSerializer;
import com.github.jelatinone.infra.aws.store.serial.JacksonS3Serializer;
import com.github.jelatinone.infra.construct.ExecutionRouter;
import com.github.jelatinone.model.audit.ExecutionStage;
import com.github.jelatinone.model.investigate.InvestigateRequest;
import com.github.jelatinone.model.struct.Identity.TargetIdentity;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class InfraContractsTest {

	@Test
	void jacksonMapper_usesIsoDateSerialization() throws Exception {
		String json = JacksonMapper.mapper.writeValueAsString(Map.of("at", StructTestFixtures.NOW));

		assertTrue(json.contains("2026-05-07T12:00:00Z"));
	}

	@Test
	void executionRouter_routesBoundStages_andRejectsInvalidPayloads() {
		List<InvestigateRequest> routed = new ArrayList<>();
		ExecutionRouter router = ExecutionRouter.of(
				ExecutionRouter.bind(ExecutionStage.INVESTIGATE, InvestigateRequest.class, routed::add));
		InvestigateRequest envelope = StructTestFixtures.request(0, ExecutionStage.INVESTIGATE);

		router.route(envelope);

		assertEquals(List.of(envelope), routed);
		assertThrows(IllegalStateException.class, () -> router.route(
				StructTestFixtures.request(0, ExecutionStage.ANNOTATE)));
	}

	@Test
	void jacksonDynamoSerializer_roundTripsRequestPayloads() throws Exception {
		JacksonDynamoSerializer<InvestigateRequest, TargetIdentity> serializer = new JacksonDynamoSerializer<>(
				InvestigateRequest.class,
				identity -> identity.identifier().toString());
		InvestigateRequest request = StructTestFixtures.request(2, ExecutionStage.INVESTIGATE);

		Map<String, AttributeValue> encoded = serializer.encodeItem(StructTestFixtures.TARGET_ID, request);
		InvestigateRequest decoded = serializer.decodeItem(encoded);

		assertEquals(StructTestFixtures.TARGET_ID.identifier().toString(), encoded.get("id").s());
		assertEquals(request, decoded);
		assertEquals(StructTestFixtures.TARGET_ID.identifier().toString(), serializer.encodeKey(
				StructTestFixtures.TARGET_ID).get("id").s());
	}

	@Test
	void jacksonSqsSerializer_roundTripsRequests() throws Exception {
		JacksonSQSSerializer<InvestigateRequest> serializer = new JacksonSQSSerializer<>(
				JacksonMapper.mapper.getTypeFactory().constructType(InvestigateRequest.class));
		InvestigateRequest envelope = StructTestFixtures.request(1, ExecutionStage.INVESTIGATE);

		String encodedBody = serializer.encodeBody(envelope);
		InvestigateRequest decoded = serializer.decode(encodedBody, Map.of());

		assertEquals(envelope, decoded);
		assertTrue(serializer.encodeAttributes(envelope).isEmpty());
	}

	@Test
	void jacksonS3Serializer_roundTripsRequests_andDefaultsMetadata() throws Exception {
		JacksonS3Serializer<InvestigateRequest, TargetIdentity> serializer = new JacksonS3Serializer<>(
				InvestigateRequest.class,
				identity -> identity.identifier().toString());
		InvestigateRequest request = StructTestFixtures.request(0, ExecutionStage.ANNOTATE);

		var encoded = serializer.encode(StructTestFixtures.TARGET_ID, request);
		InvestigateRequest decoded = serializer
				.decode(new com.github.jelatinone.infra.aws.store.serial.S3Serializer.StoredValue<>(
						StructTestFixtures.TARGET_ID,
						encoded.body(),
						encoded.contentType(),
						encoded.contentEncoding(),
						encoded.metadata()));

		assertEquals("application/json", encoded.contentType());
		assertEquals("utf-8", encoded.contentEncoding());
		assertTrue(encoded.metadata().isEmpty());
		assertEquals(request, decoded);
		assertEquals(StructTestFixtures.TARGET_ID.identifier().toString(), serializer.encodeKey(StructTestFixtures.TARGET_ID));
	}
}
