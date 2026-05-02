package com.github.jelatinone.meta.archetype;

/**
 * 
 * <h1>Action</h1>
 * 
 * {@link Archetype} helpers used to fluently compose a task archetype as a
 * composition with the use of {@link #from(Collect)
 * collect}, {@link CollectAction#through(Operate) operate}, and
 * {@link OperateAction#to(Persist) persist} dynamically.
 * 
 * @author Cody Washington
 * 
 */
public final class Action {

	private Action() {
	}

	public static <Consumes> CollectAction<Consumes> from(Collect<Consumes> source) {
		return new CollectStep<>(source);
	}

	public interface CollectAction<Consumes> {

		<Produces> OperateAction<Consumes, Produces> through(Operate<Consumes, Produces> operation);
	}

	public interface OperateAction<Consumes, Produces> {

		<Next> OperateAction<Consumes, Next> then(Operate<Produces, Next> operation);

		OperateAction<Consumes, Produces> handle(Retrieve<Consumes, Produces> recover);

		Archetype<Consumes, Produces> to(Persist<Produces> sink);
	}

	private record CollectStep<Consumes>(Collect<Consumes> source) implements CollectAction<Consumes> {

		@Override
		public <Produces> OperateAction<Consumes, Produces> through(Operate<Consumes, Produces> operation) {
			return new OperateStep<>(source, operation);
		}

	}

	private record OperateStep<Consumes, Produces>(
			Collect<Consumes> source,
			Operate<Consumes, Produces> operation) implements OperateAction<Consumes, Produces> {

		@Override
		public <Next> OperateAction<Consumes, Next> then(Operate<Produces, Next> next) {
			Operate<Consumes, Next> composed = operand -> {
				Produces previous = operation.operate(operand);
				if (previous == null) {
					return null;
				}
				return next.operate(previous);
			};
			return new OperateStep<>(source, composed);
		}

		public OperateAction<Consumes, Produces> handle(Retrieve<Consumes, Produces> recover) {
			Operate<Consumes, Produces> composed = operand -> {
				Produces previous = operation.operate(operand);
				if (previous == null) {
					return recover.recover(operand, null);
				}
				return previous;
			};
			return new OperateStep<>(source, composed);
		}

		@Override
		public Archetype<Consumes, Produces> to(Persist<Produces> persist) {
			return new Composition<Consumes, Produces>(source, operation, persist);
		}
	}
}
