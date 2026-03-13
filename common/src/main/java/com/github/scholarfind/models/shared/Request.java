package com.github.scholarfind.models.shared;

public interface Request {
  RequestHeader requestHeader();

  TargetReference target();
}
