package demo.producer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import demo.producer.support.DemoDppFactory;
import demo.producer.support.Dpp4FunDppCodecAdapter;
import demo.producer.support.Dpp4FunDppValidatorAdapter;
import demo.producer.support.RegistryRecordPayload;
import dpp.registry.client.DppRegistryClient;
import demo.registry.MockEuRegistryApplication;
import demo.repo.MockRepoApplication;
import dpp.registry.client.HttpDppRegistryClient;
import dpp.registry.client.exception.DppHttpClientException;
import dpp.registry.payloads.DppStatusCode;
import dpp.registry.payloads.RegisterDppRequest;
import dpp.repo.client.HttpDppRepoClient;
import dppsdk.dpp4fun.model.Dpp4Fun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationContextFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;

class RealServicesSmokeTest {

    private final DemoDppFactory factory = new DemoDppFactory();

    private ConfigurableApplicationContext repoContext;
    private ConfigurableApplicationContext registryContext;

    @AfterEach
    void closeContexts() {
        if (registryContext != null) {
            registryContext.close();
            registryContext = null;
        }
        if (repoContext != null) {
            repoContext.close();
            repoContext = null;
        }
    }

    @Test
    @DisplayName("Real repo and registry services support create then register with repo-backed HEAD verification")
    void realRepoAndRegistrySupportCreateThenRegisterFlow() {
        repoContext = startContext(MockRepoApplication.class);
        registryContext = startContext(MockEuRegistryApplication.class);
        clearInMemoryStore(repoContext, "inMemoryDppStore");
        clearInMemoryStore(registryContext, "inMemoryRegistryStore");

        String repoUrl = "http://localhost:" + localPort(repoContext);
        String registryUrl = "http://localhost:" + localPort(registryContext);

        HttpDppRepoClient<Dpp4Fun> repoClient = new HttpDppRepoClient<>(
                repoUrl,
                new Dpp4FunDppCodecAdapter(),
                new Dpp4FunDppValidatorAdapter()
        );
        DppRegistryClient registryClient = new HttpDppRegistryClient(registryUrl);
        MockRegistryLookupClient mockRegistryLookupClient = new MockRegistryLookupClient(
                registryUrl,
                new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules()
        );

        Dpp4Fun dpp = factory.createValidBedDpp();
        String dppId = dpp.getDppId();
        String productId = dpp.getProductId();

        assertEquals(dppId, repoClient.createDpp(dpp).getDppId());

        String registryIdentifier = registryClient.postNewDppToRegistry(
                new RegisterDppRequest(productId, dppId, "operator-123", repoUrl)
        ).getRegistryIdentifier();
        assertFalse(registryIdentifier.isBlank());

        RegistryRecordPayload registryRecord = mockRegistryLookupClient.readByDppId(dppId).orElseThrow();
        assertEquals(registryIdentifier, registryRecord.registryIdentifier());
        assertEquals(dppId, registryRecord.dppIdentifier());
        assertEquals(productId, registryRecord.productIdentifier());
        assertEquals("operator-123", registryRecord.operatorIdentifier());
        assertEquals(repoUrl, registryRecord.repoUrl());
        assertTrue(mockRegistryLookupClient.readByRegistryId(registryIdentifier).isPresent());

        DppHttpClientException missingDpp = assertThrows(
                DppHttpClientException.class,
                () -> registryClient.postNewDppToRegistry(
                        new RegisterDppRequest(productId, "missing-dpp-id", "operator-123", repoUrl)
                )
        );
        assertEquals(404, missingDpp.statusCode());
        assertTrue(missingDpp.responseBody().contains(DppStatusCode.ClientErrorResourceNotFound.name()));
    }

    @Test
    @DisplayName("HTTP demo rerun succeeds when a previous aborted run left the demo DPP in the repo")
    void httpDemoRerunSucceedsWhenPreviousRunLeftDemoDppInRepo() {
        repoContext = startContext(MockRepoApplication.class);
        registryContext = startContext(MockEuRegistryApplication.class);
        clearInMemoryStore(repoContext, "inMemoryDppStore");
        clearInMemoryStore(registryContext, "inMemoryRegistryStore");

        String repoUrl = "http://localhost:" + localPort(repoContext);
        String registryUrl = "http://localhost:" + localPort(registryContext);
        HttpDppRepoClient<Dpp4Fun> repoClient = new HttpDppRepoClient<>(
                repoUrl,
                new Dpp4FunDppCodecAdapter(),
                new Dpp4FunDppValidatorAdapter()
        );
        MockRegistryLookupClient mockRegistryLookupClient = new MockRegistryLookupClient(
                registryUrl,
                new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules()
        );

        Dpp4Fun dpp = factory.createValidBedDpp();
        String dppId = dpp.getDppId();
        String productId = dpp.getProductId();
        repoClient.createDpp(dpp);

        assertDoesNotThrow(() -> new HttpServiceDemoRunner().run(registryUrl, repoUrl));

        dpp.repo.client.exception.DppHttpClientException missingOldDpp = assertThrows(
                dpp.repo.client.exception.DppHttpClientException.class,
                () -> repoClient.readDppById(dppId)
        );
        assertEquals(404, missingOldDpp.statusCode());

        dpp.repo.client.exception.DppHttpClientException missingByProductAfterRun = assertThrows(
                dpp.repo.client.exception.DppHttpClientException.class,
                () -> repoClient.readDppByProductId(productId)
        );
        assertEquals(404, missingByProductAfterRun.statusCode());
        assertTrue(mockRegistryLookupClient.readByDppId(dppId).isEmpty());
    }

    private ConfigurableApplicationContext startContext(Class<?> applicationClass) {
        SpringApplication application = new SpringApplication(applicationClass);
        application.setWebApplicationType(WebApplicationType.SERVLET);
        application.setApplicationContextFactory(
                ApplicationContextFactory.ofContextClass(AnnotationConfigServletWebServerApplicationContext.class)
        );
        return application.run(
                "--server.port=0",
                "--debug=false",
                "--logging.level.root=WARN",
                "--logging.level.org.springframework=WARN"
        );
    }

    private int localPort(ConfigurableApplicationContext context) {
        return ((ServletWebServerApplicationContext) context).getWebServer().getPort();
    }

    private void clearInMemoryStore(ConfigurableApplicationContext context, String beanName) {
        try {
            Object bean = context.getBean(beanName);
            java.lang.reflect.Method clear = bean.getClass().getDeclaredMethod("clear");
            clear.setAccessible(true);
            clear.invoke(bean);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to reset seeded in-memory store " + beanName, exception);
        }
    }
}
