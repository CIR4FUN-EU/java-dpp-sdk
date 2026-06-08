package demo.repo;

import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;
import dppsdk.dpp4fun.validation.Dpp4FunValidationService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RepoConfiguration {

    @Bean
    Dpp4FunJsonCodec dppJsonCodec() {
        return new Dpp4FunJsonCodec();
    }

    @Bean
    Dpp4FunValidationService validationService() {
        return new Dpp4FunValidationService();
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
