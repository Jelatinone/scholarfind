package com.github.scholarfind.meta;

public enum State {

  CREATED,

  AWAITING,

  COLLECTING,

  // Sequential-exclusive States

  OPERATING,

  POSTING,

  RETRYING,

  // Parallel-exlusive States

  DISPATCHING,

  RESTARTING,

  COMPLETED,

  FAILED,

}
