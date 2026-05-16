package com.github.jelatinone.meta.construct;

import com.github.jelatinone.model.struct.Request;

public interface Router {

	void route(Request<?> envelope);
}
