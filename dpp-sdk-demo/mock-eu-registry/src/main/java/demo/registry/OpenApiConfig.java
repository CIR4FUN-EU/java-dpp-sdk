package demo.registry;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "DPP Mock Registry API",
        version = "1.0.0",
        description = "Local demo/mock registry metadata service for interactive testing. Not a production EU registry service."
), tags = {
        @Tag(name = "DPP Registry",
                description = "Public mock registry metadata operations."),
        @Tag(name = "DPP Registry - Internal",
                description = "Internal helpers for inspecting mock registry metadata.")
})
class OpenApiConfig {

    @Bean
    OpenApiCustomizer registryRequiredProperties() {
        return openApi -> {
            var schemas = openApi.getComponents().getSchemas();
            var request = schemas.get("RegisterDppRequest");
            request.setRequired(List.of(
                    "uniqueProductIdentifier",
                    "digitalProductPassportId",
                    "uniqueEconomicOperatorIdentifier",
                    "dppApiEndpoint"
            ));
            property(request, "uniqueProductIdentifier").setDescription("Unique product ID for the product associated with this DPP.");
            property(request, "uniqueProductIdentifier").setMinLength(1);
            property(request, "uniqueProductIdentifier").setPattern(".*\\S.*");
            property(request, "digitalProductPassportId").setDescription("DPP ID assigned to the passport document; distinct from the product ID.");
            property(request, "digitalProductPassportId").setMinLength(1);
            property(request, "digitalProductPassportId").setPattern(".*\\S.*");
            property(request, "uniqueEconomicOperatorIdentifier").setDescription("Unique identifier of the economic operator associated with this DPP.");
            property(request, "uniqueEconomicOperatorIdentifier").setMinLength(1);
            property(request, "uniqueEconomicOperatorIdentifier").setPattern(".*\\S.*");
            property(request, "dppApiEndpoint").setDescription("Repository API URI stored as registration metadata; the registry does not contact it.");
            property(request, "dppApiEndpoint").setFormat("uri");
            property(request, "dppApiEndpoint").setMinLength(1);
            property(request, "dppApiEndpoint").setPattern(".*\\S.*");

            addResponsePayloadSchema(openApi, "RegisterDppResponse", registrationResponseSchema());
            addResponsePayloadSchema(openApi, "RegistryRecordPayload", registryRecordSchema());
            var registrationResponse = schemas.get("RegisterDppResponse");
            if (registrationResponse != null) {
                property(registrationResponse, "registrationId").setDescription("Identifier assigned to the registry metadata record.");
            }
            var record = schemas.get("RegistryRecordPayload");
            if (record != null) {
                property(record, "registryIdentifier").setDescription("Identifier assigned to this registry metadata record.");
                property(record, "dppIdentifier").setDescription("DPP ID associated with this registration.");
                property(record, "productIdentifier").setDescription("Unique product ID associated with this DPP.");
                property(record, "operatorIdentifier").setDescription("Unique identifier of the associated economic operator.");
                property(record, "repoUrl").setDescription("Repository API URI stored as metadata; the registry does not dereference it.");
            }
            schemas.forEach((name, schema) -> {
                if (name.startsWith("DppApiResponse")) {
                    var message = property(schema, "message");
                    if (message != null) {
                        message.setNullable(true);
                    }
                    if ("DppApiResponse".equals(name)) {
                        var payload = property(schema, "payload");
                        if (payload != null) {
                            payload.setType(null);
                        }
                    }
                }
            });

            setResponseSchema(openApi, openApi.getPaths().get("/v1/registerDPP").getPost(), "201",
                    "DppApiResponseRegisterDppResponse", "RegisterDppResponse");
            setResponseSchema(openApi, openApi.getPaths().get("/internal/dpps/{registryId}").getGet(), "200",
                    "DppApiResponseRegistryRecordPayload", "RegistryRecordPayload");
            setResponseSchema(openApi, openApi.getPaths().get("/internal/dpps/by-dpp-id/{dppId}").getGet(), "200",
                    "DppApiResponseRegistryRecordPayload", "RegistryRecordPayload");
        };
    }

    private static Schema<?> property(Schema<?> schema, String name) {
        return (Schema<?>) schema.getProperties().get(name);
    }

    private static Schema<?> registrationResponseSchema() {
        ObjectSchema schema = new ObjectSchema();
        schema.addProperty("registrationId", new StringSchema());
        schema.setRequired(List.of("registrationId"));
        return schema;
    }

    private static Schema<?> registryRecordSchema() {
        ObjectSchema schema = new ObjectSchema();
        schema.addProperty("registryIdentifier", new StringSchema());
        schema.addProperty("dppIdentifier", new StringSchema());
        schema.addProperty("productIdentifier", new StringSchema());
        schema.addProperty("operatorIdentifier", new StringSchema());
        schema.addProperty("repoUrl", new StringSchema());
        StringSchema registeredAt = new StringSchema();
        registeredAt.setFormat("date-time");
        schema.addProperty("registeredAt", registeredAt);
        StringSchema lastUpdatedAt = new StringSchema();
        lastUpdatedAt.setFormat("date-time");
        schema.addProperty("lastUpdatedAt", lastUpdatedAt);
        schema.setRequired(List.of("registryIdentifier", "dppIdentifier", "productIdentifier", "operatorIdentifier",
                "repoUrl", "registeredAt", "lastUpdatedAt"));
        return schema;
    }

    private static void addResponsePayloadSchema(OpenAPI openApi, String name, Schema<?> payload) {
        openApi.getComponents().addSchemas(name, payload);
    }

    private static void setResponseSchema(OpenAPI openApi, Operation operation, String responseCode,
            String wrapperName, String payloadName) {
        ComposedSchema response = new ComposedSchema();
        response.addAllOfItem(reference("DppApiResponse"));
        ObjectSchema payloadOverride = new ObjectSchema();
        payloadOverride.addProperty("payload", reference(payloadName));
        response.addAllOfItem(payloadOverride);
        openApi.getComponents().addSchemas(wrapperName, response);
        operation.getResponses().get(responseCode).getContent().get("application/json").setSchema(reference(wrapperName));
    }

    private static Schema<?> reference(String name) {
        Schema<Object> schema = new Schema<>();
        schema.set$ref("#/components/schemas/" + name);
        return schema;
    }
}
