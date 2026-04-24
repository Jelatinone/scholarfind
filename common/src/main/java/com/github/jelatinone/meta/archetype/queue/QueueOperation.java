package com.github.jelatinone.meta.archetype.queue;

import com.github.jelatinone.api.Envelope;
import com.github.jelatinone.meta.archetype.Operate;
import com.github.jelatinone.meta.archetype.Retrieve;
import com.github.jelatinone.meta.result.OperationResult;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QueueOperation<Consumes, Produces> implements Operate<Envelope<Consumes>, Envelope<Produces>> {

	Operate<Consumes, Produces> operation;
	Retrieve<Consumes, Produces> recovery;

	@Override
	public OperationResult<Envelope<Produces>> operate(Envelope<Consumes> operand) {
		try {
			OperationResult<Produces> output = operation.operate(operand.content());
			return new OperationResult<>(new Envelope<>(output.value(), operand.acknowledgement()));
		} catch (Throwable throwable) {
			Produces recovered = recovery.recover(operand.content(), throwable);
			return new OperationResult<>(new Envelope<>(recovered, operand.acknowledgement()));
		}
	}

}
