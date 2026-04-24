package com.github.jelatinone.meta.archetype.queue;

import com.github.jelatinone.meta.transitory.Directive;

public interface QueueDisposition<Output> {

	Directive directive(Output output);

	void complete(Output output) throws Exception;

	void retry(Output output) throws Exception;

	void error(Output output) throws Exception;
}
