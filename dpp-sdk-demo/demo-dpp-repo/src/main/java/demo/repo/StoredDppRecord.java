package demo.repo;

import java.time.Instant;

record StoredDppRecord(
        String dppId,
        String productId,
        String dppJson,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        boolean deleted
) {

    StoredDppRecord withUpdatedJson(String updatedJson, Instant updatedAt) {
        return new StoredDppRecord(dppId, productId, updatedJson, createdAt, updatedAt, deletedAt, deleted);
    }

    StoredDppRecord withDeleted(Instant deletedAt) {
        return new StoredDppRecord(dppId, productId, dppJson, createdAt, deletedAt, deletedAt, true);
    }
}
