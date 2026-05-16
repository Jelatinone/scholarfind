package com.github.jelatinone.infra;

import com.github.jelatinone.acquisition.Acquirer;
import com.github.jelatinone.meta.construct.Infrastructure;
import com.github.jelatinone.model.investigate.InvestigateDocument;

public interface InvestigateInfrastructure extends Infrastructure<InvestigateDocument> {

	Acquirer acquirer();
}
