package com.github.jelatinone.model.scholarship.dossier.description;

import java.math.BigDecimal;

import lombok.NonNull;

public record Award(
		@NonNull BigDecimal minimumAmount,
		@NonNull BigDecimal maximumAmount,

		String currencyCode,

		boolean recipientRenewable,
		int recipientCount,

		String descriptor) {
}
