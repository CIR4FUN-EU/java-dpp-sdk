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

    T readDppByProductId(String productId);

    T readDppVersionByProductIdAndDate(String productId, Instant date);

    ReadDppIdsResponse readDppIdsByProductIds(List<String> productIds, Integer limit, String cursor);

    T updateDppById(String dppId, JsonNode partialDpp);

    DeleteDppResponse deleteDppById(String dppId);

    JsonNode readDataElement(String dppId, String elementPath);

    JsonNode updateDataElement(String dppId, String elementPath, JsonNode payload);
}
