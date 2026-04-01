package com.github.jelatinone.api;

public interface Acknowledgement {

  void success();

  void retry();

  void error();

}
