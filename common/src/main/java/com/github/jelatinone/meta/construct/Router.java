package com.github.jelatinone.meta.construct;

import com.github.jelatinone.model.transit.Letter;

public interface Router {

	void route(Letter<?> envelope);

}
