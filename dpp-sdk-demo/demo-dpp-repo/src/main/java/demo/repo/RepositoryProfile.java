package demo.repo;

import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.semantic.SemanticResolver;

/** Startup-selected domain validation behavior over the canonical passport model. */
interface RepositoryProfile {
    String id();
    void validateProfile(DigitalProductPassport passport);
    default SemanticResolver semanticResolver() { return null; }
}
