package demo.repo;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "DPP Mock Repository API",
        version = "1.0.0",
        description = "Local demo/mock DPP repository service for interactive testing. Not a production repository service."
), tags = {
        @Tag(name = "DPP Repository - Life Cycle API",
                description = "Mock lifecycle operations for full DPP create, read, update, and delete behavior."),
        @Tag(name = "DPP Repository - Fine Granular API",
                description = "Mock fine-granular DPP element read and update operations."),
        @Tag(name = "DPP Repository - Internal",
                description = "Internal helpers used by the mock services and local diagnostics.")
})
class OpenApiConfig {

    @Bean
    OpenApiCustomizer repositoryRequiredProperties() {
        return openApi -> openApi.getComponents().getSchemas().get("ReadDppIdsRequest")
                .setRequired(List.of("productIdentifiers"));
    }
}
