package com.github.jelatinone.model.archive;

import java.math.BigDecimal;

public record ScholarshipAward(
		String descriptor,
		BigDecimal minimumAmount,
		BigDecimal maximumAmount,
		String currencyCode,
		Integer recipientCount,
		Boolean renewable) {
}
