package com.github.scholarfind.meta;

public enum State {

  CREATED,

  AWAITING,

  COLLECTING,

  RESTARTING,

  COMPLETED,

  FAILED,

  // Sequential-exclusive States

  OPERATING,

  POSTING,

  RETRYING,

  // Parallel-exlusive States

  DISPATCHING,

}
