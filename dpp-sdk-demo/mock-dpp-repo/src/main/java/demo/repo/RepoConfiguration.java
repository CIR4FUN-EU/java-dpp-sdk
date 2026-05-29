package demo.repo;

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
            if (!repoService.hasActiveDpp(PostmanSeedData.DPP_ID)) {
                repoService.create(dppJsonCodec.toJson(PostmanSeedData.createSeedDpp()));
            }
        };
    }
}
