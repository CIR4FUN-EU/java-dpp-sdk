CREATE TABLE IF NOT EXISTS registry_records (
    registry_identifier VARCHAR(64) PRIMARY KEY,
    dpp_identifier VARCHAR(255) NOT NULL,
    product_identifier VARCHAR(255) NOT NULL,
    operator_identifier VARCHAR(255) NOT NULL,
    repo_url VARCHAR(2048) NOT NULL,
    registered_at TIMESTAMPTZ NOT NULL,
    last_updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_registry_records_dpp_identifier
    ON registry_records (dpp_identifier);
