package com.github.jelatinone.models.shared;

public interface Request {
  RequestHeader requestHeader();

  TargetReference target();
}
