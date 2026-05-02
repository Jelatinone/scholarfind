package com.github.jelatinone.meta.archetype.pipeline;

import com.github.jelatinone.meta.archetype.Operate;
import com.github.jelatinone.model.struct.Document;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.transit.Letter;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PipelineOperation<In extends Request, Out extends Request, Context, State, Doc extends Document<Doc>>
    implements Operate<Letter<In>, PipelineResult<Doc, In>> {

  PipelineArchetype<In, Out, Context, State, Doc> archetype;

  @Override
  public PipelineResult<Doc, In> operate(Letter<In> operand) {
    return archetype.processPipeline(operand);
  }
}
