package demo.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;

@SpringBootApplication
public class MockEuRegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(MockEuRegistryApplication.class, args);
    }

    @Bean
    ApplicationRunner seedDefaultPostmanRegistryRecord(
            InMemoryRegistryStore store,
            @Value("${demo.repo.public-base-url:http://localhost:${DPP_REPO_PORT:8080}}") String repoUrl
    ) {
        return args -> store.seed(
                PostmanSeedData.REGISTRY_ID,
                PostmanSeedData.PRODUCT_ID,
                PostmanSeedData.DPP_ID,
                PostmanSeedData.OPERATOR_ID,
                repoUrl,
                PostmanSeedData.REGISTERED_AT
        );
    }
}
