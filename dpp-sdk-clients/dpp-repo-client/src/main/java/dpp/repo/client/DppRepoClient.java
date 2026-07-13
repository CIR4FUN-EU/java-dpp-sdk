package dpp.repo.client;

import com.fasterxml.jackson.databind.JsonNode;
import dpp.repo.payloads.CreateDppResponse;
import dpp.repo.payloads.DeleteDppResponse;
import dpp.repo.payloads.ReadDppIdsResponse;

import java.time.Instant;
import java.util.List;

/**
 * Public client interface for repository lifecycle and fine-granular DPP operations.
 *
 * @param <T> caller-owned DPP model type
 */
public interface DppRepoClient<T> {
    CreateDppResponse createDpp(T dpp);

    T readDppById(String dppId);

    JsonNode readCompressedDppById(String dppId);

    T readDppByProductId(String productId);

    T readDppVersionByIdAndDate(String dppId, Instant date);

    /**
     * @deprecated Non-standard transitional compatibility method. EN 18222-facing callers should
     * use {@link #readDppVersionByIdAndDate(String, Instant)}.
     */
    @Deprecated
    T readDppVersionByProductIdAndDate(String productId, Instant date);

    ReadDppIdsResponse readDppIdsByProductIds(List<String> productIds, Integer limit, String cursor);

    T updateDppById(String dppId, JsonNode partialDpp);

    DeleteDppResponse deleteDppById(String dppId);

    JsonNode readDataElement(String dppId, String elementIdPath);

    JsonNode updateDataElement(String dppId, String elementIdPath, JsonNode dataElement);
}
