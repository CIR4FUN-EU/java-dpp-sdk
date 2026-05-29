package demo.registry;

import dpp.registry.payloads.DppApiResponse;
import dpp.registry.payloads.DppStatusCode;
import dpp.registry.payloads.RegisterDppRequest;
import dpp.registry.payloads.RegisterDppResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the mock EC registry metadata API.
 *
 * <p>The controller handles only registration metadata. It does not store or return full DPP JSON.
 * DPP existence is verified through the repository verifier and service registration flow before
 * metadata is stored.</p>
 */
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
class DppRegistryController {

    private final DppRegistryService registryService;
    private final ApiResponseFactory responseFactory;

    DppRegistryController(DppRegistryService registryService, ApiResponseFactory responseFactory) {
        this.registryService = registryService;
        this.responseFactory = responseFactory;
    }

    @PostMapping(value = "/registerDPP", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DppApiResponse<RegisterDppResponse>> register(@RequestBody RegisterDppRequest request) {
        return responseFactory.success(HttpStatus.CREATED, DppStatusCode.SuccessCreated, registryService.register(request));
    }

    @GetMapping("/registry/dpps/{registryId}")
    ResponseEntity<DppApiResponse<RegistryRecordPayload>> readByRegistryId(@PathVariable String registryId) {
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success, registryService.readByRegistryId(registryId));
    }

    @GetMapping("/registry/dpps/by-dpp-id/{dppId}")
    ResponseEntity<DppApiResponse<RegistryRecordPayload>> readByDppId(@PathVariable String dppId) {
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success, registryService.readByDppId(dppId));
    }

    @GetMapping("/health")
    HealthPayload health() {
        return new HealthPayload("UP", "mock-eu-registry");
    }
}
