create table if not exists dpp_passports (
    id bigserial primary key,
    dpp_id varchar(255) not null unique,
    product_id varchar(255) not null,
    created_at timestamptz not null,
    deleted_at timestamptz null
);

create table if not exists dpp_repository_profile (
    singleton boolean primary key default true check (singleton),
    repository_profile varchar(64) not null,
    initialized_at timestamptz not null default now()
);

create unique index if not exists uq_dpp_passports_active_product_id
    on dpp_passports (product_id)
    where deleted_at is null;

create table if not exists dpp_versions (
    id bigserial primary key,
    passport_id bigint not null references dpp_passports(id) on delete restrict,
    version_no bigint not null check (version_no > 0),
    status varchar(32) not null check (status in ('ACTIVE', 'SUPERSEDED', 'DELETED')),
    valid_from timestamptz not null,
    valid_to timestamptz null,
    stored_at timestamptz not null default now(),
    operation_id varchar(255) null,
    content jsonb not null,
    content_hash varchar(64) not null check (content_hash ~ '^[0-9a-f]{64}$'),
    check (valid_to is null or valid_from < valid_to),
    check ((status = 'ACTIVE' and valid_to is null) or (status <> 'ACTIVE' and valid_to is not null)),
    unique (passport_id, version_no)
);

create unique index if not exists uq_dpp_versions_active_passport
    on dpp_versions (passport_id)
    where status = 'ACTIVE';

create index if not exists ix_dpp_versions_passport_history
    on dpp_versions (passport_id, version_no);

create index if not exists ix_dpp_versions_passport_valid_from
    on dpp_versions (passport_id, valid_from desc);
