create table if not exists dpp4fun_classifications (
    version_id bigint primary key references dpp_versions(id) on delete cascade,
    sector varchar(255) not null,
    group_name varchar(255) null,
    category varchar(255) not null,
    sub_category varchar(255) null
);

create table if not exists dpp4fun_classification_tags (
    version_id bigint not null references dpp_versions(id) on delete cascade,
    order_index integer not null,
    tag varchar(255) not null,
    primary key (version_id, order_index)
);

create table if not exists dpp4fun_characteristics (
    version_id bigint primary key references dpp_versions(id) on delete cascade,
    product_name varchar(255) not null,
    description text null,
    brand varchar(255) null,
    product_type varchar(255) null,
    weight double precision null,
    color varchar(255) null
);

create table if not exists dpp4fun_dimensions (
    version_id bigint primary key references dpp_versions(id) on delete cascade,
    width double precision not null,
    height double precision not null,
    depth double precision not null,
    unit varchar(64) null
);

create table if not exists dpp4fun_features (
    version_id bigint not null references dpp_versions(id) on delete cascade,
    order_index integer not null,
    feature varchar(255) not null,
    primary key (version_id, order_index)
);

create table if not exists dpp4fun_bill_of_materials (
    version_id bigint primary key references dpp_versions(id) on delete cascade
);

create table if not exists dpp4fun_materials (
    version_id bigint not null references dpp_versions(id) on delete cascade,
    order_index integer not null,
    name varchar(255) not null,
    mandatory boolean not null,
    portion double precision not null,
    reference varchar(255) null,
    primary key (version_id, order_index)
);

create table if not exists dpp4fun_components (
    version_id bigint not null references dpp_versions(id) on delete cascade,
    order_index integer not null,
    name varchar(255) not null,
    reference varchar(255) null,
    primary key (version_id, order_index)
);

create table if not exists dpp4fun_parts (
    version_id bigint not null references dpp_versions(id) on delete cascade,
    order_index integer not null,
    name varchar(255) not null,
    mandatory boolean not null,
    reference varchar(255) null,
    primary key (version_id, order_index)
);
