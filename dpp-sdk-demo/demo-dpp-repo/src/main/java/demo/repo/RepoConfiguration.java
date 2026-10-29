package demo.repo;

import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.core.transport.json.DppJsonCodec;
import dppsdk.postgres.core.PostgresDppRepository;
import dppsdk.postgres.events.PostgresDppRepositoryWithEvents;
import dppsdk.postgres.dpp4fun.Dpp4FunPostgresRepository;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;

/**
 * Wiring for the demo repository backend.
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
    DppJsonCodec dppJsonCodec() {
        return new DppJsonCodec();
    }

    @Bean
    @ConditionalOnProperty(name = "dpp.repo.backend", havingValue = "memory", matchIfMissing = true)
    DppRepoBackend inMemoryRepoBackend(InMemoryDppStore store, DppJsonCodec dppJsonCodec,
                                       com.fasterxml.jackson.databind.ObjectMapper objectMapper,
                                       @Value("${dpp.repo.events.enabled:true}") boolean eventsEnabled) {
        return new InMemoryDppRepoBackend(store, dppJsonCodec, objectMapper, eventsEnabled);
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
    PostgresDppRepository postgresDppRepository(DataSource dataSource, RepositoryProfiles profiles) {
        PostgresDppRepository repository = new PostgresDppRepository(dataSource);
        repository.initializeRepositoryProfile(profiles.selected().id());
        return repository;
    }

    @Bean
    @ConditionalOnProperty(name = "dpp.repo.backend", havingValue = "postgres")
    DppRepoBackend postgresRepoBackend(DataSource dataSource, PostgresDppRepository repository,
            RepositoryProfiles profiles, com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            @Value("${dpp.repo.events.enabled:true}") boolean eventsEnabled) {
        RepositoryProfile profile = profiles.selected();
        if (profile.id().equals("dpp4fun")) {
            Dpp4FunPostgresRepository dpp4FunRepository = new Dpp4FunPostgresRepository(dataSource, eventsEnabled);
            // This opt-in demo backend explicitly bootstraps its required schemas before seed operations.
            dpp4FunRepository.initializeSchema();
            return new PostgresDppRepoBackend(dpp4FunRepository, objectMapper);
        }
        PostgresDppRepositoryWithEvents events = null;
        if (eventsEnabled) {
            events = new PostgresDppRepositoryWithEvents(dataSource);
            events.initializeSchema();
        }
        return new CorePostgresDppRepoBackend(repository, events, objectMapper);
    }

    @Bean
    ApplicationRunner seedDefaultPostmanDpp(DppRepoService repoService,
                                             DppJsonCodec dppJsonCodec, RepositoryProfiles profiles) {
        return args -> {
            if (!profiles.selected().id().equals("dpp4fun")) return;
            seedIfMissing(repoService, dppJsonCodec, PostmanSeedData.DPP_ID, PostmanSeedData.createSeedDpp());
            seedIfMissing(repoService, dppJsonCodec, PostmanSeedData.DELETE_EXAMPLE_DPP_ID, PostmanSeedData.createDeleteExampleDpp());
            seedIfMissing(repoService, dppJsonCodec, PostmanSeedData.LIFECYCLE_DEFAULT_DPP_ID, PostmanSeedData.createLifecycleDefaultDpp());
            seedIfMissing(repoService, dppJsonCodec, PostmanSeedData.LIFECYCLE_DELETE_DPP_ID, PostmanSeedData.createLifecycleDeleteDpp());
            seedIfMissing(repoService, dppJsonCodec, PostmanSeedData.FINE_GRAINED_DEFAULT_DPP_ID, PostmanSeedData.createFineGrainedDefaultDpp());
            seedIfMissing(repoService, dppJsonCodec, PostmanSeedData.FINE_GRAINED_DELETE_DPP_ID, PostmanSeedData.createFineGrainedDeleteDpp());
            seedIfMissing(repoService, dppJsonCodec, PostmanSeedData.REGISTRY_DELETE_DPP_ID, PostmanSeedData.createRegistryDeleteDpp());
            seedHistoricalSwaggerDppIfMissing(repoService);
        };
    }

    private static void seedIfMissing(DppRepoService repoService, DppJsonCodec dppJsonCodec, String dppId, dppsdk.dpp4fun.model.Dpp4Fun dpp) {
        if (!repoService.hasAnyDpp(dppId)) {
            repoService.create(dppJsonCodec.toExpandedJson(dpp.asDigitalProductPassport()));
        }
    }

    private static void seedHistoricalSwaggerDppIfMissing(DppRepoService repoService) {
        if (!repoService.hasAnyDpp(PostmanSeedData.HISTORICAL_SWAGGER_DPP_ID)) {
            repoService.createAt(PostmanSeedData.createHistoricalSwaggerDpp().asDigitalProductPassport(),
                    PostmanSeedData.HISTORICAL_SWAGGER_VALID_FROM);
        }
    }

}
