package com.github.scholarfind.api;

public interface Acknowledgement {

  void success();

  void retry();

  void error();

}
