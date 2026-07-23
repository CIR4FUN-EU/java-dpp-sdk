package demo.registry;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.tags.Tag;
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
        return openApi -> openApi.getComponents().getSchemas().get("RegisterDppRequest").setRequired(List.of(
                "uniqueProductIdentifier",
                "digitalProductPassportId",
                "uniqueEconomicOperatorIdentifier",
                "dppApiEndpoint"
        ));
    }
}
