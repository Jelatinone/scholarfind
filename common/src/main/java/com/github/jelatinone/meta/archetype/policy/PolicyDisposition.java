package com.github.jelatinone.meta.archetype.policy;

public interface PolicyDisposition<Input, Context, State, Document> {

	void next(PolicyResult<Input, Context, State, Document> result) throws Exception;

	void drop(PolicyResult<Input, Context, State, Document> result) throws Exception;

	void retry(PolicyResult<Input, Context, State, Document> result) throws Exception;

	void error(PolicyResult<Input, Context, State, Document> result) throws Exception;
}
