create table if not exists dpp_passports (
    id bigserial primary key,
    dpp_id varchar(255) not null unique,
    product_id varchar(255) not null,
    passport_type varchar(255) not null,
    created_at timestamptz not null,
    deleted_at timestamptz null
);

create unique index if not exists uq_dpp_passports_active_product_id
    on dpp_passports (product_id)
    where deleted_at is null;

create table if not exists dpp_versions (
    id bigserial primary key,
    passport_id bigint not null references dpp_passports(id) on delete cascade,
    version_no bigint not null check (version_no > 0),
    status varchar(32) not null check (status in ('ACTIVE', 'SUPERSEDED', 'DELETED')),
    valid_from timestamptz not null,
    valid_to timestamptz null,
    stored_at timestamptz not null default now(),
    operation_id varchar(255) null,
    unique (passport_id, version_no)
);

create unique index if not exists uq_dpp_versions_active_passport
    on dpp_versions (passport_id)
    where status = 'ACTIVE';

create table if not exists dpp_passport_metadata (
    version_id bigint primary key references dpp_versions(id) on delete cascade,
    unique_product_identifier uuid not null,
    qr_code_or_digital_tag text null,
    external_documentation_link text null
);

create table if not exists dpp_passport_update_dates (
    version_id bigint not null references dpp_versions(id) on delete cascade,
    order_index integer not null,
    update_date date not null,
    primary key (version_id, order_index)
);

create table if not exists dpp_addresses (
    id bigserial primary key,
    version_id bigint not null references dpp_versions(id) on delete cascade,
    country varchar(255) not null,
    zip_code varchar(255) null,
    region varchar(255) null,
    town varchar(255) not null,
    street varchar(255) null
);

create table if not exists dpp_emails (
    id bigserial primary key,
    version_id bigint not null references dpp_versions(id) on delete cascade,
    email_address varchar(255) not null,
    type_of_email varchar(255) null
);

create table if not exists dpp_telephones (
    id bigserial primary key,
    version_id bigint not null references dpp_versions(id) on delete cascade,
    telephone_number varchar(255) not null,
    type_of_telephone varchar(255) null
);

create table if not exists dpp_contacts (
    id bigserial primary key,
    version_id bigint not null references dpp_versions(id) on delete cascade,
    organization_name varchar(255) not null,
    address_id bigint null references dpp_addresses(id) on delete set null,
    email_id bigint null references dpp_emails(id) on delete set null,
    telephone_id bigint null references dpp_telephones(id) on delete set null
);

create table if not exists dpp_organizations (
    id bigserial primary key,
    version_id bigint not null references dpp_versions(id) on delete cascade,
    slot_name varchar(32) not null,
    name varchar(255) not null,
    gln varchar(255) null,
    product_description varchar(255) null,
    product_designation varchar(255) null,
    product_family varchar(255) null,
    product_root varchar(255) null,
    product_order_suffix varchar(255) null,
    uri varchar(1024) null,
    role varchar(32) null,
    contact_id bigint null references dpp_contacts(id) on delete set null
);

create unique index if not exists uq_dpp_organizations_version_slot
    on dpp_organizations (version_id, slot_name);

create table if not exists dpp_nameplates (
    version_id bigint primary key references dpp_versions(id) on delete cascade,
    gtin_code varchar(255) not null,
    internal_article_number varchar(255) null,
    batch_number varchar(255) null,
    customs_tariff_number varchar(255) null,
    uri_of_the_product varchar(1024) null,
    manufacturer_organization_id bigint null references dpp_organizations(id) on delete set null,
    supplier_organization_id bigint null references dpp_organizations(id) on delete set null
);

create table if not exists dpp_documentation (
    version_id bigint primary key references dpp_versions(id) on delete cascade,
    digital_instructions_link varchar(1024) null,
    safety_instructions_link varchar(1024) null,
    downloadable boolean not null,
    available_for_years integer null,
    paper_copy_available_on_request boolean not null
);

create table if not exists dpp_lifecycle_events (
    id bigserial primary key,
    event_id varchar(255) not null unique,
    dpp_id varchar(255) not null,
    event_type varchar(64) not null,
    occurred_at timestamptz not null,
    data jsonb not null
);
