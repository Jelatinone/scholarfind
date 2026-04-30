package com.github.jelatinone.model.archive.scholarship.dossier.description;

import java.math.BigDecimal;

public record Award(
		BigDecimal minimumAmount,
		BigDecimal maximumAmount,
		String currencyCode,
		Integer recipientCount,
		Boolean renewable) {
}
