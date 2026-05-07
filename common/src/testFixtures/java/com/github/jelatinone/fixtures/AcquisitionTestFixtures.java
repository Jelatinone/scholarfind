package com.github.jelatinone.fixtures;

import com.github.jelatinone.acquisition.Acquisition;
import com.github.jelatinone.model.content.MediaEncoding;
import com.github.jelatinone.model.content.MediaMetadata;
import com.github.jelatinone.model.content.MediaType;

public final class AcquisitionTestFixtures {

	private AcquisitionTestFixtures() {
	}

	public static MediaMetadata mediaMetadata(String contentType, long contentLength) {
		return new MediaMetadata(200, 0, 0, contentType, contentLength);
	}

	public static Acquisition.Interpreted acquisition(
			byte[] sourceBytes,
			MediaType mediaType,
			MediaEncoding mediaEncoding,
			MediaMetadata mediaMetadata) {
		return new com.github.jelatinone.acquisition.Acquisition.Interpreted(
				StructTestFixtures.TARGET_ID,
				StructTestFixtures.REVIEW_ID,
				CanonicalTestFixtures.url("https://example.com/content"),
				mediaType,
				mediaEncoding,
				mediaMetadata,
				StructTestFixtures.NOW,
				sourceBytes,
				"source-hash");
	}

}
