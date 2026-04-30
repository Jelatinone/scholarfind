package com.github.jelatinone.model.scholarship.dossier.description;

import java.math.BigDecimal;
import java.math.BigInteger;

public record Award(
		BigDecimal minimumAmount,
		BigDecimal maximumAmount,

		String currencyCode,

		Boolean recipientRenewable,
		BigInteger recipientCount,

		String descriptor) {
}
