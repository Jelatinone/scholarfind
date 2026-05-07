package com.github.jelatinone.acquisition;

import lombok.NonNull;

public interface Acquirer {

	Acquisition.Metadata metadata(@NonNull Acquisition current);

	Acquisition.Interpreted interpreted(@NonNull Acquisition current);
}
