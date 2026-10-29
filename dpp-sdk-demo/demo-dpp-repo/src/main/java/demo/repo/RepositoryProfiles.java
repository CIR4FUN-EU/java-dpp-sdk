package demo.repo;

import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.semantic.SemanticResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Discovers profile adapters and resolves the configured profile once during startup. */
@Component
final class RepositoryProfiles {
    private final RepositoryProfile core;
    private final RepositoryProfile selected;

    RepositoryProfiles(@Value("${dpp.repo.profile:dpp4fun}") String configuredProfile,
                       Collection<RepositoryProfile> availableProfiles) {
        Map<String, RepositoryProfile> profiles = new TreeMap<>();
        for (RepositoryProfile profile : availableProfiles) {
            String id = profile.id().toLowerCase(Locale.ROOT);
            RepositoryProfile previous = profiles.putIfAbsent(id, profile);
            if (previous != null) {
                throw new IllegalStateException("Duplicate repository profile id '" + id + "'");
            }
        }
        core = profiles.get("core");
        if (core == null) {
            throw new IllegalStateException("The core repository profile adapter is required");
        }

        String profileId = configuredProfile == null || configuredProfile.isBlank()
                ? "dpp4fun" : configuredProfile.trim().toLowerCase(Locale.ROOT);
        selected = profiles.get(profileId);
        if (selected == null) {
            throw new IllegalArgumentException("Unknown dpp.repo.profile '" + configuredProfile
                    + "'. Available profiles: " + String.join(", ", profiles.keySet()));
        }
    }

    RepositoryProfile selected() {
        return selected;
    }

    SemanticResolver semanticResolver() { return selected.semanticResolver(); }

    void validate(DigitalProductPassport passport) {
        core.validateProfile(passport);
        if (selected != core) {
            selected.validateProfile(passport);
        }
    }
}
