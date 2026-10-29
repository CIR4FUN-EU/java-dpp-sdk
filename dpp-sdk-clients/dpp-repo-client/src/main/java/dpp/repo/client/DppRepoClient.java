package dpp.repo.client;

import com.fasterxml.jackson.databind.JsonNode;
import dpp.repo.payloads.CreateDppResponse;
import dpp.repo.payloads.DeleteDppResponse;
import dpp.repo.payloads.DppApiResponse;
import dpp.repo.payloads.DppRepresentation;
import dpp.repo.payloads.ReadDppIdsResponse;

import java.time.Instant;
import java.util.List;

/** Public client interface for repository lifecycle and fine-granular DPP operations. */
public interface DppRepoClient {
    /** Creates a DPP from caller-supplied neutral JSON. The default wire representation is compressed. */
    CreateDppResponse createDpp(JsonNode dpp);

    /** Creates a DPP from caller-supplied neutral JSON using the selected EN 18223 representation. */
    CreateDppResponse createDpp(JsonNode dpp, DppRepresentation representation);

    DppApiResponse<JsonNode> readDppById(String dppId);

    DppApiResponse<JsonNode> readDppById(String dppId, DppRepresentation representation);

    DppApiResponse<JsonNode> readDppByProductId(String productId);

    DppApiResponse<JsonNode> readDppByProductId(String productId, DppRepresentation representation);

    DppApiResponse<JsonNode> readDppVersionByIdAndDate(String dppId, Instant date);

    DppApiResponse<JsonNode> readDppVersionByIdAndDate(String dppId, Instant date, DppRepresentation representation);

    ReadDppIdsResponse readDppIdsByProductIds(List<String> productIds, Integer limit, String cursor);

    DppApiResponse<JsonNode> updateDppById(String dppId, JsonNode partialDpp);

    DppApiResponse<JsonNode> updateDppById(String dppId, JsonNode partialDpp, DppRepresentation representation);

    DeleteDppResponse deleteDppById(String dppId);

    DppApiResponse<JsonNode> readDataElement(String dppId, String elementIdPath);

    DppApiResponse<JsonNode> readDataElement(String dppId, String elementIdPath, DppRepresentation representation);

    DppApiResponse<JsonNode> updateDataElement(String dppId, String elementIdPath, JsonNode dataElement);

    DppApiResponse<JsonNode> updateDataElement(String dppId, String elementIdPath, JsonNode dataElement,
            DppRepresentation representation);
}
