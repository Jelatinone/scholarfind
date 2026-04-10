package com.github.jelatinone.meta.construct;

import com.github.jelatinone.models.shared.StageEnvelope;

public interface Router {

	void route(StageEnvelope<?> envelope);

}
