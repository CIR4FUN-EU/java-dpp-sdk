package dpp.registry.client;

import dpp.registry.payloads.RegisterDppRequest;
import dpp.registry.payloads.RegisterDppResponse;

/**
 * Public client interface for registry registration.
 */
public interface DppRegistryClient {
    RegisterDppResponse postNewDppToRegistry(RegisterDppRequest request);
}
