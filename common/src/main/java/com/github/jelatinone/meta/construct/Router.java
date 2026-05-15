package com.github.jelatinone.meta.construct;

import org.jsoup.helper.HttpConnection.Request;

public interface Router {

	void route(Request envelope);
}
