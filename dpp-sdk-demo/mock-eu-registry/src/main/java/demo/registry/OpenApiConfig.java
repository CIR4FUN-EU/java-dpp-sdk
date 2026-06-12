package demo.registry;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "DPP Mock Registry API",
        version = "1.0.0",
        description = "Local demo/mock registry metadata service for interactive testing. Not a production EU registry service."
))
class OpenApiConfig {
}
