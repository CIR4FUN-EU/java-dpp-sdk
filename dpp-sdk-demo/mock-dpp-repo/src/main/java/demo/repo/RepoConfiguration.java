package demo.repo;

import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;
import dppsdk.dpp4fun.validation.Dpp4FunValidationService;
import dppsdk.postgres.dpp4fun.Dpp4FunPostgresRepository;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.Clock;

/**
 * Wiring for the mock repository backend.
 *
 * <p>Memory remains the default backend. PostgreSQL is opt-in through {@code dpp.repo.backend=postgres}.</p>
 */
@Configuration
class RepoConfiguration {

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    Dpp4FunJsonCodec dppJsonCodec() {
        return new Dpp4FunJsonCodec();
    }

    @Bean
    Dpp4FunValidationService validationService() {
        return new Dpp4FunValidationService();
    }

    @Bean
    @ConditionalOnProperty(name = "dpp.repo.backend", havingValue = "memory", matchIfMissing = true)
    DppRepoBackend inMemoryRepoBackend(InMemoryDppStore store, Dpp4FunJsonCodec dppJsonCodec,
                                       com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return new InMemoryDppRepoBackend(store, dppJsonCodec, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "dpp.repo.backend", havingValue = "postgres")
    DataSource postgresDataSource(
            @org.springframework.beans.factory.annotation.Value("${spring.datasource.url}") String url,
            @org.springframework.beans.factory.annotation.Value("${spring.datasource.username}") String username,
            @org.springframework.beans.factory.annotation.Value("${spring.datasource.password}") String password
    ) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(url);
        dataSource.setUser(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    @Bean
    @ConditionalOnProperty(name = "dpp.repo.backend", havingValue = "postgres")
    Dpp4FunPostgresRepository dpp4FunPostgresRepository(DataSource dataSource) {
        return new Dpp4FunPostgresRepository(dataSource);
    }

    @Bean
    @ConditionalOnProperty(name = "dpp.repo.backend", havingValue = "postgres")
    DppRepoBackend postgresRepoBackend(Dpp4FunPostgresRepository repository,
                                       com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return new PostgresDppRepoBackend(repository, objectMapper);
    }

    @Bean
    ApplicationRunner seedDefaultPostmanDpp(DppRepoService repoService, Dpp4FunJsonCodec dppJsonCodec) {
        return args -> {
            seedIfMissing(repoService, dppJsonCodec, PostmanSeedData.DPP_ID, PostmanSeedData.createSeedDpp());
            seedIfMissing(repoService, dppJsonCodec, PostmanSeedData.DELETE_EXAMPLE_DPP_ID, PostmanSeedData.createDeleteExampleDpp());
            seedIfMissing(repoService, dppJsonCodec, PostmanSeedData.LIFECYCLE_DEFAULT_DPP_ID, PostmanSeedData.createLifecycleDefaultDpp());
            seedIfMissing(repoService, dppJsonCodec, PostmanSeedData.LIFECYCLE_DELETE_DPP_ID, PostmanSeedData.createLifecycleDeleteDpp());
            seedIfMissing(repoService, dppJsonCodec, PostmanSeedData.FINE_GRAINED_DEFAULT_DPP_ID, PostmanSeedData.createFineGrainedDefaultDpp());
            seedIfMissing(repoService, dppJsonCodec, PostmanSeedData.FINE_GRAINED_DELETE_DPP_ID, PostmanSeedData.createFineGrainedDeleteDpp());
            seedIfMissing(repoService, dppJsonCodec, PostmanSeedData.REGISTRY_DELETE_DPP_ID, PostmanSeedData.createRegistryDeleteDpp());
        };
    }

    private static void seedIfMissing(DppRepoService repoService, Dpp4FunJsonCodec dppJsonCodec, String dppId, Dpp4Fun dpp) {
        if (!repoService.hasActiveDpp(dppId)) {
            repoService.create(dppJsonCodec.toJson(dpp));
        }
    }
}
