package com.github.scholarfind.validation;

import java.util.EnumSet;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ValidatorResult {

    EnumSet<Capability> _capabilities = EnumSet.allOf(Capability.class);
    EnumSet<Reason> _reasons = EnumSet.noneOf(Reason.class);

    @NonFinal
    boolean unprocessable = false;

    public synchronized void deny(final Capability capability, final Reason reason) {
        _capabilities.remove(capability);
        _reasons.add(reason);
    }

    public void fail(Reason reason) {
        unprocessable = true;
        _reasons.add(reason);
    }

    public EnumSet<Capability> capabilities() {
        return _capabilities;
    }

    public EnumSet<Reason> reasons() {
        return _reasons;
    }

    public boolean processable() {
        return unprocessable;
    }
}
