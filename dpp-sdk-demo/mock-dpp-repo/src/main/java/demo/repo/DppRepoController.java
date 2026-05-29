package demo.repo;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import dpp.repo.payloads.CreateDppResponse;
import dpp.repo.payloads.DppApiResponse;
import dpp.repo.payloads.DppStatusCode;
import dpp.repo.payloads.ReadDppIdsRequest;
import dpp.repo.payloads.ReadDppIdsResponse;
import dpp.repo.payloads.UpdateDataElementRequest;

/**
 * Exposes the standard-style mock repository API.
 *
 * <p>This controller stays thin: it accepts HTTP payloads, delegates lifecycle behavior to the service,
 * and wraps responses in the shared API envelope.</p>
 */
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
class DppRepoController {

    private final DppRepoService repoService;
    private final ApiResponseFactory responseFactory;

    DppRepoController(DppRepoService repoService, ApiResponseFactory responseFactory) {
        this.repoService = repoService;
        this.responseFactory = responseFactory;
    }

    @PostMapping(value = "/dpps", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DppApiResponse<CreateDppResponse>> create(@RequestBody String jsonPayload) {
        return responseFactory.success(HttpStatus.CREATED, DppStatusCode.SuccessCreated, repoService.create(jsonPayload));
    }

    @GetMapping("/dpps/{dppId}")
    ResponseEntity<DppApiResponse<JsonNode>> readById(@PathVariable String dppId) {
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success, repoService.readById(dppId));
    }

    @RequestMapping(value = "/dpps/{dppId}", method = RequestMethod.HEAD)
    ResponseEntity<Void> verifyById(@PathVariable String dppId) {
        return repoService.hasActiveDpp(dppId)
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/dppsByProductId/{productId}")
    ResponseEntity<DppApiResponse<JsonNode>> readByProductId(@PathVariable String productId) {
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success, repoService.readByProductId(productId));
    }

    @GetMapping("/dppsByProductIdAndDate/{productId}")
    ResponseEntity<DppApiResponse<JsonNode>> readByProductIdAndDate(
            @PathVariable String productId,
            @RequestParam("date") String date
    ) {
        // Snapshot lookup is based on repository history, not on any version fields inside the DPP document.
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success,
                repoService.readVersionByProductIdAndDate(productId, date));
    }

    @PostMapping(value = "/dppsByProductIds", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DppApiResponse<ReadDppIdsResponse>> readIdsByProductIds(
            @RequestBody ReadDppIdsRequest request
    ) {
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success, repoService.readIdsByProductIds(request));
    }

    @PatchMapping(value = "/dpps/{dppId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DppApiResponse<JsonNode>> update(@PathVariable String dppId, @RequestBody String patchJson) {
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success, repoService.updateById(dppId, patchJson));
    }

    @DeleteMapping("/dpps/{dppId}")
    ResponseEntity<DppApiResponse<Void>> delete(@PathVariable String dppId) {
        // Deletes are soft deletes so lifecycle history and historical snapshots remain queryable.
        repoService.deleteById(dppId);
        return responseFactory.successNoContent();
    }

    @GetMapping("/dpps/{dppId}/elements/{elementPath}")
    ResponseEntity<DppApiResponse<JsonNode>> readDataElement(
            @PathVariable String dppId,
            @PathVariable String elementPath
    ) {
        // Element paths are a deliberately small supported subset, not arbitrary JSONPath.
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success,
                repoService.readDataElement(dppId, elementPath));
    }

    @PatchMapping(value = "/dpps/{dppId}/elements/{elementPath}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DppApiResponse<JsonNode>> updateDataElement(
            @PathVariable String dppId,
            @PathVariable String elementPath,
            @RequestBody UpdateDataElementRequest request
    ) {
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success,
                repoService.updateDataElement(dppId, elementPath, request));
    }

    @GetMapping("/dpps/{dppId}/events")
    ResponseEntity<DppApiResponse<List<LifecycleEventRecord>>> readEvents(@PathVariable String dppId) {
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success, repoService.readEvents(dppId));
    }

    @GetMapping("/health")
    HealthPayload health() {
        return new HealthPayload("UP", "mock-dpp-repo");
    }
}
