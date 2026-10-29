package demo.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import dppsdk.core.model.DataElement;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.model.DppHeader;
import dppsdk.core.model.Granularity;
import dppsdk.core.model.SingleValuedDataElement;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.model.characteristics.ChairCharacteristics;
import dppsdk.dpp4fun.semantic.Dpp4FunSemanticProfile;
import dpp.repo.payloads.DppRepresentation;
import dppsdk.core.transport.json.DppJsonCodec;
import dppsdk.core.validation.ValidationException;
import dpp.repo.payloads.DppStatusCode;
import demo.repo.testsupport.DemoDppFactory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepositoryProfileTest {
    @Test
    void coreProfileAcceptsGenericCanonicalPassportAndDpp4FunRequiresItsProfileRules() {
        DigitalProductPassport generic = genericPassport("generic-1", "product-1");
        assertDoesNotThrow(() -> profiles("core").validate(generic));
        assertDoesNotThrow(() -> profiles("dpp4fun").validate(generic),
                "unknown or absent content specifications retain generic best-effort behavior");
    }

    @Test
    void validDpp4FunPassportUsesItsComposedCoreAndProfileValidation() {
        DigitalProductPassport dpp = new DemoDppFactory().createValidBedDpp().asDigitalProductPassport();
        assertDoesNotThrow(() -> profiles("dpp4fun").validate(dpp));
    }

    @Test
    void memoryRepositoryRunsCoreAndDpp4FunProfilesThroughCanonicalServiceBoundary() {
        DigitalProductPassport generic = genericPassport("memory-profile-1", "memory-profile-product");
        DppJsonCodec codec = new DppJsonCodec();
        InMemoryDppStore coreStore = new InMemoryDppStore();
        InMemoryDppRepoBackend coreBackend = new InMemoryDppRepoBackend(coreStore, codec, new ObjectMapper(), true);
        DppRepoService coreService = service(coreBackend, codec, profiles("core"));
        coreService.create(codec.toExpandedJson(generic));
        assertEquals(generic, coreBackend.findCurrentByDppId("memory-profile-1").orElseThrow());

        InMemoryDppRepoBackend funBackend = new InMemoryDppRepoBackend(
                new InMemoryDppStore(), codec, new ObjectMapper(), true);
        DppRepoService funService = service(funBackend, codec, profiles("dpp4fun"));
        funService.create(codec.toExpandedJson(generic));
        DigitalProductPassport dpp4fun = new DemoDppFactory().createValidBedDpp().asDigitalProductPassport();
        funService.create(codec.toExpandedJson(dpp4fun));
        assertEquals(dpp4fun, funBackend.findCurrentByDppId(DemoDppFactory.BED_DPP_ID).orElseThrow());
    }

    @Test
    void selectedDpp4FunProfileRejectsInvalidBoundSemanticsAfterCoreValidation() {
        var profile = Dpp4FunSemanticProfile.loadDefault();
        var characteristics = new dppsdk.core.model.DataElementCollection.Builder().elementId("characteristics")
                .dataElements(List.of(new dppsdk.core.model.MultiValuedDataElement.Builder().elementId("color")
                        .dataElements(List.of(single("0", "blue"))).build())).build();
        DigitalProductPassport invalid = new DigitalProductPassport.Builder()
                .dppHeader(new DemoDppFactory().createValidBedDpp().getHeader().toBuilder()
                        .contentSpecificationIds(profile.contentSpecificationIds(
                                dppsdk.dpp4fun.model.characteristics.Dpp4FunProductVariant.CHAIR)).build())
                .dataElements(List.of(characteristics)).build();
        assertThrows(ValidationException.class, () -> profiles("dpp4fun").validate(invalid));
    }

    @Test
    void compressedDpp4FunCreateUsesSelectedCatalogResolverAndStoresCanonicalPassport() {
        Dpp4Fun typed = Dpp4Fun.builder(new DemoDppFactory().createValidBedDpp().getHeader())
                .chairCharacteristics(ChairCharacteristics.builder().color("blue").build()).build();
        DigitalProductPassport expected = typed.asDigitalProductPassport();
        DppJsonCodec codec = new DppJsonCodec();
        InMemoryDppRepoBackend backend = new InMemoryDppRepoBackend(
                new InMemoryDppStore(), codec, new ObjectMapper(), true);
        DppRepoService service = service(backend, codec, profiles("dpp4fun"));

        service.create(codec.toCompressedJson(expected), DppRepresentation.COMPRESSED);

        DigitalProductPassport actual = backend.findCurrentByDppId(expected.getDppHeader()
                .getDigitalProductPassportId()).orElseThrow();
        assertEquals(expected, actual);
    }

    @Test
    void compressedFineGrainedDpp4FunUpdateUsesFullSemanticPath() throws Exception {
        Dpp4Fun typed = Dpp4Fun.builder(new DemoDppFactory().createValidBedDpp().getHeader())
                .chairCharacteristics(ChairCharacteristics.builder().color("blue").build()).build();
        DigitalProductPassport expected = typed.asDigitalProductPassport();
        DppJsonCodec codec = new DppJsonCodec();
        InMemoryDppRepoBackend backend = new InMemoryDppRepoBackend(
                new InMemoryDppStore(), codec, new ObjectMapper(), true);
        DppRepoService service = service(backend, codec, profiles("dpp4fun"));
        service.create(codec.toExpandedJson(expected), DppRepresentation.FULL);
        String elementPath = "$['elements'][0]['elements'][0]";

        service.updateDataElement(expected.getDppHeader().getDigitalProductPassportId(), elementPath,
                new ObjectMapper().readTree("\"green\""), DppRepresentation.COMPRESSED);

        DigitalProductPassport actual = backend.findCurrentByDppId(expected.getDppHeader()
                .getDigitalProductPassportId()).orElseThrow();
        var actualElement = (dppsdk.core.model.SingleValuedDataElement)
                ((dppsdk.core.model.DataElementCollection) actual.getDataElements().get(0)).getDataElements().get(0);
        var expectedElement = (dppsdk.core.model.SingleValuedDataElement)
                ((dppsdk.core.model.DataElementCollection) expected.getDataElements().get(0)).getDataElements().get(0);
        assertEquals("green", actualElement.getValue());
        assertEquals(expectedElement.getDictionaryReference(), actualElement.getDictionaryReference());
        assertEquals(expectedElement.getValueDataType(), actualElement.getValueDataType());
    }

    @Test
    void everyProfileStillRunsCoreValidationAndUnknownValuesFailInsteadOfFallingBack() {
        DigitalProductPassport invalidCore = genericPassport(null, "product-2");
        assertThrows(ValidationException.class, () -> profiles("core").validate(invalidCore));
        assertThrows(ValidationException.class, () -> profiles("dpp4fun").validate(invalidCore));
        IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class,
                () -> profiles("batery"));
        assertEquals(true, unknown.getMessage().contains("core, dpp4fun"));
        assertEquals("dpp4fun", profiles(null).selected().id());
    }

    @Test
    void profileCanBeAddedByRegisteringAnAdapterWithoutChangingCentralSelectionCode() {
        RepositoryProfile battery = new RepositoryProfile() {
            @Override public String id() { return "battery"; }
            @Override public void validateProfile(DigitalProductPassport passport) { }
        };
        List<RepositoryProfile> adapters = new ArrayList<>(List.of(
                new CoreRepositoryProfile(), new Dpp4FunRepositoryProfile(), battery));
        RepositoryProfiles selected = new RepositoryProfiles("battery", adapters);
        assertEquals("battery", selected.selected().id());
        assertThrows(ValidationException.class,
                () -> selected.validate(genericPassport(null, "invalid-core")));
    }

    @Test
    void memoryEventsCanBeDisabledWithoutDisablingCanonicalHistory() {
        DppJsonCodec codec = new DppJsonCodec();
        InMemoryDppStore store = new InMemoryDppStore();
        InMemoryDppRepoBackend backend = new InMemoryDppRepoBackend(store, codec, new ObjectMapper(), false);
        DigitalProductPassport first = genericPassport("memory-1", "memory-product");
        backend.create(first, Instant.parse("2026-01-01T00:00:00Z"));
        DigitalProductPassport next = new DigitalProductPassport.Builder().dppHeader(first.getDppHeader())
                .dataElements(List.of(single("generic", "updated"))).build();
        backend.appendVersion(next, 1, Instant.parse("2026-01-01T00:01:00Z"), "DPP_UPDATED", java.util.Map.of());

        assertEquals(2, store.versionsForProduct("memory-product").size());
        assertEquals(List.of(), store.eventsFor("memory-1"));
        assertEquals(next, backend.findCurrentByDppId("memory-1").orElseThrow());
        RepoApiException eventsUnavailable = assertThrows(RepoApiException.class,
                () -> service(backend, codec, profiles("core"), false).readEvents("memory-1"));
        assertEquals(DppStatusCode.ServerNotImplemented, eventsUnavailable.statusCode());
    }

    private static DigitalProductPassport genericPassport(String dppId, String productId) {
        DppHeader header = new DppHeader.Builder().digitalProductPassportId(dppId)
                .uniqueProductIdentifier(productId).granularity(Granularity.MODEL).dppSchemaVersion("1")
                .dppStatus("active").lastUpdated(Instant.parse("2026-01-01T00:00:00Z"))
                .economicOperatorId("operator").build();
        return new DigitalProductPassport.Builder().dppHeader(header)
                .dataElements(List.of(single("generic", "value"))).build();
    }

    private static DataElement single(String id, String value) {
        return new SingleValuedDataElement.Builder().elementId(id).value(value).build();
    }

    private static RepositoryProfiles profiles(String configuredProfile) {
        return new RepositoryProfiles(configuredProfile,
                List.of(new CoreRepositoryProfile(), new Dpp4FunRepositoryProfile()));
    }

    private static DppRepoService service(DppRepoBackend backend, DppJsonCodec codec, RepositoryProfiles profiles,
                                          boolean eventsEnabled) {
        return new DppRepoService(backend, codec, profiles, eventsEnabled, new ObjectMapper(),
                new DppIdentifierExtractor(), new DppMergePatchService(), new DppElementPathService(),
                Clock.systemUTC());
    }

    private static DppRepoService service(DppRepoBackend backend, DppJsonCodec codec, RepositoryProfiles profiles) {
        return service(backend, codec, profiles, true);
    }
}
