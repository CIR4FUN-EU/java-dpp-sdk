package demo.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import demo.repo.testsupport.DemoDppFactory;
import dppsdk.core.transport.json.DppJsonCodec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {"dpp.repo.profile=dpp4fun", "dpp.repo.backend=memory"})
@AutoConfigureMockMvc
class CompressedRepresentationControllerTest {
    @Autowired MockMvc mvc;
    @Autowired InMemoryDppStore store;
    @Autowired ObjectMapper json;

    @Test
    void defaultCreateAndPatchUseSdkCompressedRepresentationAndPreserveStoredMetadata() throws Exception {
        store.clear();
        var passport = new DemoDppFactory().createValidBedDpp().asDigitalProductPassport();
        var codec = new DppJsonCodec();
        String id = passport.getDppHeader().getDigitalProductPassportId();
        var compressed = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(codec.toCompressedJson(passport));

        mvc.perform(post("/v1/dpps").contentType(MediaType.APPLICATION_JSON).content(compressed.toString()))
                .andExpect(status().isCreated());
        mvc.perform(get("/v1/dpps/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.elements").doesNotExist());

        var patch = json.createObjectNode();
        patch.put("newOperationalValue", "added");
        mvc.perform(patch("/v1/dpps/{id}", id).contentType(MediaType.APPLICATION_JSON).content(patch.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.newOperationalValue").value("added"));
        mvc.perform(get("/v1/dpps/{id}", id).param("representation", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.elements").isArray());
    }

    @Test
    void fineGranularDefaultReadsAndUpdatesOperationalValue() throws Exception {
        store.clear();
        var passport = new DemoDppFactory().createValidBedDpp().asDigitalProductPassport();
        var codec = new DppJsonCodec();
        String id = passport.getDppHeader().getDigitalProductPassportId();
        mvc.perform(post("/v1/dpps").param("representation", "full")
                .contentType(MediaType.APPLICATION_JSON).content(codec.toExpandedJson(passport)))
                .andExpect(status().isCreated());

        String path = DemoDppFactory.BRAND_PATH;
        mvc.perform(get("/v1/dpps/{id}/elements/{path}", id, path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").value("Cir4Fun Platform Bed"));
        mvc.perform(patch("/v1/dpps/{id}/elements/{path}", id, path)
                .contentType(MediaType.APPLICATION_JSON).content("\"Changed name\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").value("Changed name"));
    }

    @Test
    void compressedPatchRetainsFullMetadataAndRejectsInvalidRootAtomically() throws Exception {
        store.clear();
        var passport = new DemoDppFactory().createValidBedDpp().asDigitalProductPassport();
        var full = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(
                new DppJsonCodec().toExpandedJson(passport));
        var extension = json.createObjectNode();
        extension.put("elementId", "vendorCompatibleExtension");
        extension.put("objectType", "SingleValuedDataElement");
        extension.put("dictionaryReference", "https://example.org/dictionary/vendor-extension");
        extension.put("valueDataType", "xsd:string");
        extension.put("value", "stored extension value");
        full.withArray("elements").add(extension);
        String id = passport.getDppHeader().getDigitalProductPassportId();
        mvc.perform(post("/v1/dpps").param("representation", "full")
                .contentType(MediaType.APPLICATION_JSON).content(full.toString()))
                .andExpect(status().isCreated());

        mvc.perform(patch("/v1/dpps/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nameplate\":{\"brand\":\"Changed\"}}"))
                .andExpect(status().isOk());
        var fullResponse = json.readTree(mvc.perform(get("/v1/dpps/{id}", id)
                .param("representation", "full")).andReturn().getResponse().getContentAsString());
        var storedExtension = findElement(fullResponse.path("payload"), "vendorCompatibleExtension");
        assertEquals("https://example.org/dictionary/vendor-extension",
                storedExtension.path("dictionaryReference").asText());
        assertEquals("xsd:string", storedExtension.path("valueDataType").asText());
        assertEquals("stored extension value", storedExtension.path("value").asText());
        mvc.perform(get("/v1/dpps/{id}", id).param("representation", "full"))
                .andExpect(jsonPath(DemoDppFactory.BRAND_PATH.replace("$", "$.payload") + ".value")
                        .value("Changed"));

        int revisionCount = store.versionsForProduct("04012345678901").size();
        mvc.perform(patch("/v1/dpps/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
                .andExpect(status().isBadRequest());
        assertEquals(revisionCount, store.versionsForProduct("04012345678901").size());
    }

    @Test
    void explicitCompressedCreateAndPatchUseTheSameOperationalTransport() throws Exception {
        store.clear();
        var passport = new DemoDppFactory().createValidBedDpp().asDigitalProductPassport();
        String id = passport.getDppHeader().getDigitalProductPassportId();
        mvc.perform(post("/v1/dpps").param("representation", "compressed")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new DppJsonCodec().toCompressedJson(passport)))
                .andExpect(status().isCreated());
        mvc.perform(patch("/v1/dpps/{id}", id).param("representation", "compressed")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newOperationalValue\":\"explicit\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.newOperationalValue").value("explicit"));
        mvc.perform(get("/v1/dpps/{id}", id).param("representation", "full"))
                .andExpect(jsonPath("$.payload.elements").isArray());
    }

    @Test
    void emptyingStoredCollectionViaCompressedPatchLeavesCanonicalStateUntouched() throws Exception {
        store.clear();
        var passport = new DemoDppFactory().createValidBedDpp().asDigitalProductPassport();
        var codec = new DppJsonCodec();
        String id = passport.getDppHeader().getDigitalProductPassportId();
        String productId = passport.getDppHeader().getUniqueProductIdentifier();
        mvc.perform(post("/v1/dpps").param("representation", "full")
                .contentType(MediaType.APPLICATION_JSON).content(codec.toExpandedJson(passport)))
                .andExpect(status().isCreated());
        int revisionCount = store.versionsForProduct(productId).size();
        int eventCount = store.eventsFor(id).size();

        var compressed = json.readTree(codec.toCompressedJson(passport));
        var removeChildren = json.createObjectNode();
        compressed.path("characteristics").fieldNames().forEachRemaining(removeChildren::putNull);
        var patch = json.createObjectNode();
        patch.set("characteristics", removeChildren);
        mvc.perform(patch("/v1/dpps/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(patch.toString()))
                .andExpect(status().isBadRequest());

        assertEquals(revisionCount, store.versionsForProduct(productId).size());
        assertEquals(eventCount, store.eventsFor(id).size());
        mvc.perform(get("/v1/dpps/{id}", id).param("representation", "full"))
                .andExpect(jsonPath("$.payload.elements[19].objectType").value("DataElementCollection"));
    }

    private com.fasterxml.jackson.databind.JsonNode findElement(
            com.fasterxml.jackson.databind.JsonNode node, String elementId) {
        if (node == null) return null;
        if (node.isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode child : node) {
                var found = findElement(child, elementId);
                if (found != null) return found;
            }
        } else if (node.isObject()) {
            if (elementId.equals(node.path("elementId").asText())) return node;
            var found = findElement(node.get("elements"), elementId);
            if (found == null) found = findElement(node.get("value"), elementId);
            if (found != null) return found;
        }
        return null;
    }
}
