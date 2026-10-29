package dppsdk.postgres.core;

import dppsdk.core.model.DigitalProductPassport;

import java.util.Objects;

/** One current canonical passport and the version expected by a subsequent optimistic write. */
public record PostgresDppSnapshot(DigitalProductPassport passport, long versionNo) {
    public PostgresDppSnapshot {
        Objects.requireNonNull(passport, "passport must not be null");
        if (versionNo < 1) throw new IllegalArgumentException("versionNo must be positive");
    }
}
