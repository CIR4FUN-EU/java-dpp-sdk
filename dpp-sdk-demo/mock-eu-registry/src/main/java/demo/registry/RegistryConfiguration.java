package demo.registry;

import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Optional backend selection for the mock registry service.
 *
 * <p>Memory mode remains the default and does not require any datasource configuration.</p>
 */
@Configuration
class RegistryConfiguration {

    @Bean
    @ConditionalOnProperty(name = "dpp.registry.backend", havingValue = "memory", matchIfMissing = true)
    RegistryBackend inMemoryRegistryBackend(InMemoryRegistryStore store) {
        return new InMemoryRegistryBackend(store);
    }

    @Bean
    @ConditionalOnProperty(name = "dpp.registry.backend", havingValue = "postgres")
    DataSource postgresDataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password
    ) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(url);
        dataSource.setUser(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    @Bean
    @ConditionalOnProperty(name = "dpp.registry.backend", havingValue = "postgres")
    RegistryBackend postgresRegistryBackend(DataSource dataSource) {
        return new PostgresRegistryBackend(dataSource);
    }

    @Bean
    ApplicationRunner seedDefaultPostmanRegistryRecord(
            RegistryBackend backend,
            @Value("${demo.repo.public-base-url:http://localhost:${MOCK_REPO_PORT:${DPP_REPO_PORT:8080}}}") String repoUrl
    ) {
        return args -> backend.seed(
                PostmanSeedData.REGISTRY_ID,
                PostmanSeedData.PRODUCT_ID,
                PostmanSeedData.DPP_ID,
                PostmanSeedData.OPERATOR_ID,
                repoUrl,
                PostmanSeedData.REGISTERED_AT
        );
    }
}
