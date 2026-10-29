package demo.repo;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.math.BigDecimal;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "DPP Demo Repository API",
        version = "1.0.0",
        description = "Local demo DPP repository service for interactive testing. Not a production repository service."
), tags = {
        @Tag(name = "DPP Repository - Life Cycle API",
                description = "Lifecycle operations for full DPP create, read, update, and delete behavior."),
        @Tag(name = "DPP Repository - Fine Granular API",
                description = "Fine-granular DPP element read and update operations."),
        @Tag(name = "DPP Repository - Internal",
                description = "Internal helpers used by the demo services and local diagnostics.")
})
class OpenApiConfig {

    @Bean
    OpenApiCustomizer repositoryRequiredProperties() {
        return openApi -> {
            var schemas = openApi.getComponents().getSchemas();
            var request = schemas.get("ReadDppIdsRequest");
            request.setRequired(List.of("productIdentifiers"));
            property(request, "productIdentifiers").setDescription("Product IDs to match against active DPP records.");
            property(request, "productIdentifiers").setMinItems(1);
            property(request, "limit").setDescription("Maximum number of matching DPP IDs to return; defaults to 50.");
            property(request, "limit").setMinimum(BigDecimal.ONE);
            property(request, "cursor").setDescription("Zero-based offset into the matching DPP IDs, encoded as a non-negative integer.");
            property(request, "cursor").setPattern("^\\+?\\d+$");

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

            addResponsePayloadSchema(openApi, "CreateDppResponse", createDppResponseSchema());
            setResponseSchema(openApi, openApi.getPaths().get("/v1/dpps").getPost(), "201",
                    "DppApiResponseCreateDppResponse", "CreateDppResponse");

            addResponsePayloadSchema(openApi, "ReadDppIdsResponse", readDppIdsResponseSchema());
            setResponseSchema(openApi, openApi.getPaths().get("/v1/dppsByProductIds").getPost(), "200",
                    "DppApiResponseReadDppIdsResponse", "ReadDppIdsResponse");

            addResponsePayloadSchema(openApi, "LifecycleEventRecord", lifecycleEventSchema());
            ArraySchema events = new ArraySchema();
            events.setItems(reference("LifecycleEventRecord"));
            addResponsePayloadSchema(openApi, "LifecycleEventList", events);
            setResponseSchema(openApi, openApi.getPaths().get("/internal/dpps/{dppId}/events").getGet(), "200",
                    "DppApiResponseLifecycleEventList", "LifecycleEventList");
        };
    }

    private static Schema<?> createDppResponseSchema() {
        ObjectSchema schema = new ObjectSchema();
        schema.addProperty("digitalProductPassportId", new StringSchema());
        schema.setRequired(List.of("digitalProductPassportId"));
        return schema;
    }

    private static Schema<?> readDppIdsResponseSchema() {
        ObjectSchema schema = new ObjectSchema();
        ArraySchema ids = new ArraySchema();
        ids.setItems(new StringSchema());
        schema.addProperty("dppIdentifiers", ids);
        StringSchema nextCursor = new StringSchema();
        nextCursor.setNullable(true);
        schema.addProperty("nextCursor", nextCursor);
        schema.setRequired(List.of("dppIdentifiers", "nextCursor"));
        return schema;
    }

    private static Schema<?> lifecycleEventSchema() {
        ObjectSchema schema = new ObjectSchema();
        schema.addProperty("eventId", new StringSchema());
        schema.addProperty("dppId", new StringSchema());
        schema.addProperty("eventType", new StringSchema());
        StringSchema occurredAt = new StringSchema();
        occurredAt.setFormat("date-time");
        schema.addProperty("occurredAt", occurredAt);
        schema.addProperty("data", new Schema<>());
        schema.setRequired(List.of("eventId", "dppId", "eventType", "occurredAt"));
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

    private static Schema<?> property(Schema<?> schema, String name) {
        return (Schema<?>) schema.getProperties().get(name);
    }
}
