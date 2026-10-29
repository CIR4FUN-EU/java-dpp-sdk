create table if not exists dpp_lifecycle_events (
    id bigserial primary key,
    event_id varchar(255) not null unique,
    dpp_id varchar(255) not null,
    version_no bigint null,
    operation_id varchar(255) null,
    event_type varchar(64) not null,
    occurred_at timestamptz not null,
    data jsonb not null
);
alter table dpp_lifecycle_events add column if not exists version_no bigint null;
alter table dpp_lifecycle_events add column if not exists operation_id varchar(255) null;
create index if not exists ix_dpp_lifecycle_events_history
    on dpp_lifecycle_events (dpp_id, occurred_at, id);
