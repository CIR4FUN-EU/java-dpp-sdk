package demo.producer;

import com.fasterxml.jackson.databind.JsonNode;
import demo.producer.support.RegistryRecordPayload;
import dpp.registry.payloads.RegisterDppResponse;
import dpp.repo.payloads.CreateDppResponse;
import dpp.repo.payloads.DeleteDppResponse;
import dpp.repo.payloads.ReadDppIdsResponse;
import dppsdk.dpp4fun.model.Dpp4Fun;

/**
 * Small console formatting helper for the demo runners.
 *
 * <p>This keeps the demo output readable without introducing any application logging or UI concerns
 * into the runner logic itself.</p>
 */
final class ConsoleSupport {

    private ConsoleSupport() {
    }

    static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    static void step(String title) {
        System.out.println();
        System.out.println("-- " + title + " --");
    }

    static void createResponse(CreateDppResponse response) {
        System.out.println("dppId      : " + response.getDppId());
    }

    static void deleteResponse(DeleteDppResponse response) {
        System.out.println("statusCode : " + response.getStatusCode());
    }

    static void idsResponse(ReadDppIdsResponse response) {
        System.out.println("dppIds     : " + response.getDppIdentifiers());
        System.out.println("nextCursor : " + response.getNextCursor());
    }

    static void registryResponse(RegisterDppResponse response) {
        System.out.println("registryId : " + response.getRegistryIdentifier());
    }

    static void registryRecord(RegistryRecordPayload response) {
        System.out.println("registryId : " + response.registryIdentifier());
        System.out.println("dppId      : " + response.dppIdentifier());
        System.out.println("productId  : " + response.productIdentifier());
        System.out.println("operatorId : " + response.operatorIdentifier());
        System.out.println("repoUrl    : " + response.repoUrl());
    }

    static void dpp(Dpp4Fun dpp) {
        System.out.println("Product    : " + dpp.getCharacteristics().getProductName());
        System.out.println("DPP ID     : " + dpp.getPassportMetadata().getUniqueProductIdentifier());
        System.out.println("Category   : " + dpp.getClassification().getCategory());
        System.out.println("Maker      : " + dpp.getNameplate().getManufacturer().getName());
        System.out.println("Core      : passportMetadata + nameplate"
                + (dpp.getDocumentation() != null ? " + documentation" : ""));
    }

    static void jsonValue(String label, JsonNode payload) {
        System.out.println(label + " : " + payload);
    }

    static void clientError(RuntimeException exception) {
        System.out.println("clientError: " + exception.getClass().getSimpleName());
        System.out.println("message    : " + exception.getMessage());
        if (exception instanceof dpp.repo.client.exception.DppHttpClientException httpException) {
            System.out.println("httpStatus : " + httpException.statusCode());
            System.out.println("body       : " + httpException.responseBody());
        } else if (exception instanceof dpp.registry.client.exception.DppHttpClientException httpException) {
            System.out.println("httpStatus : " + httpException.statusCode());
            System.out.println("body       : " + httpException.responseBody());
        }
    }
}
