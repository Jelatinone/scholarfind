package com.github.jelatinone.acquisition;

import lombok.NonNull;

public interface AcquisitionService {

  AcquiredContent ensureMetadata(@NonNull AcquiredContent current);

  AcquiredContent ensureContent(@NonNull AcquiredContent current);

  AcquiredContent ensureText(@NonNull AcquiredContent current);
}
