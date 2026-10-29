package demo.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
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

    private static final ObjectWriter JSON_WRITER = new ObjectMapper().writer(
            new DefaultPrettyPrinter().withArrayIndenter(new DefaultIndenter("  ", System.lineSeparator()))
    );

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
        System.out.println("dppId      : " + response.getDigitalProductPassportId());
    }

    static void deleteResponse(DeleteDppResponse response) {
        System.out.println("statusCode : " + response.getStatusCode());
    }

    static void idsResponse(ReadDppIdsResponse response) {
        System.out.println("dppIds     : " + response.getDppIdentifiers());
        System.out.println("nextCursor : " + response.getNextCursor());
    }

    static void registryResponse(RegisterDppResponse response) {
        System.out.println("registryId : " + response.getRegistrationId());
    }

    static void dpp(Dpp4Fun dpp) {
        System.out.println("Product    : " + dpp.getNameplate().getBrand());
        System.out.println("DPP ID     : " + dpp.getDigitalProductPassportId());
        System.out.println("Product ID : " + dpp.getUniqueProductIdentifier());
        System.out.println("Variant    : " + dpp.getCharacteristics().variant());
        System.out.println("Maker      : " + dpp.getNameplate().getManufacturerName());
        System.out.println("Canonical : DigitalProductPassport header + DataElements");
    }

    static void compressedDpp(JsonNode dpp) {
        jsonValue("COMPRESSED DPP (EN 18223 elementId-keyed JSON)", dpp);
    }

    static void jsonValue(String label, JsonNode payload) {
        System.out.println(label + ":");
        printJson(payload);
    }

    static void jsonPathValue(String label, String path, JsonNode payload) {
        System.out.println(label + " @ " + path + ":");
        printJson(payload);
    }

    private static void printJson(JsonNode payload) {
        try {
            System.out.println(JSON_WRITER.writeValueAsString(payload));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not format demo JSON payload", exception);
        }
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
