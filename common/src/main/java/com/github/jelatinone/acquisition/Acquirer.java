package com.github.jelatinone.acquisition;

import lombok.NonNull;

public interface Acquirer {

	Acquisition acquire(@NonNull Acquisition current);

	Acquisition.Metadata metadata(@NonNull Acquisition current);

	Acquisition.Interpreted interpreted(@NonNull Acquisition current);
}
