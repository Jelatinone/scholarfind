package com.github.scholarfind.api.queue;

public interface Acknowledgement {

	void success();

	void retry();

	void error();

}
