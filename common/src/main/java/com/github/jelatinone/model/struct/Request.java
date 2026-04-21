package com.github.jelatinone.model.struct;

import java.util.UUID;

import com.github.jelatinone.model.graph.TargetNode;

/**
 * 
 * <h1>Request</h1>
 * 
 * Represents the intermediate request made to perform a stage
 * operation on a given {@link TargetNode node}.
 * 
 * @author Cody Washington
 */
public interface Request {

	RequestHeader requestHeader();

	UUID targetId();

	UUID reviewId();

}
