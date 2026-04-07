package com.github.jelatinone.task.signal;

public record SignalPattern(
    double positiveMetadataCost,
    double negativeMetadataCost,

    double hydratedContentCost,
    double snapshotContentCost,
    double negativeContentCost,

    double completeTextCost,
    double snapshotTextCost,
    double hydratedTextCost,
    double negativeTextCost) {

  public static final SignalPattern AGGRESIVE = new SignalPattern(0, 0.5D, 0, 0.5D, 1D, 0, 0.25D, 0.5D, 1D);
  public static final SignalPattern DEFAULT = new SignalPattern(0, 1D, 0, 1D, 4D, 0, 1D, 2D, 4D);
  public static final SignalPattern CONSERVATIVE = new SignalPattern(0, 3D, 0, 2D, 6D, 0, 1D, 3D, 7D);
}
