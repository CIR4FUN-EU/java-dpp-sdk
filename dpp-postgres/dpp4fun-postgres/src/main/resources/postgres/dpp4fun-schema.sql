-- Optional Dpp4Fun indexes for the generic dpp_projections current-state table.
-- Apply explicitly through PostgresProjectionSchemaManager or deployment tooling.
drop index if exists ix_dpp_proj_dpp4fun_sector_btree_text_0c3f974a484b73f5;
drop index if exists ix_dpp_proj_dpp4fun_category_btree_text_876061bae2ed5fbf;
drop index if exists ix_dpp_proj_dpp4fun_producttype_btree_text_f23347b9c8c62d9e;
create index if not exists ix_dpp_proj_dpp4fun_brand_btree_text_2495dca0910ff7f0 on dpp_projections using btree ((data ->> 'brand')) where projection_name = 'dpp4fun';
create index if not exists ix_dpp_proj_dpp4fun_materials_gin_db09c21f1241fca9 on dpp_projections using gin ((data -> 'materials')) where projection_name = 'dpp4fun';
create index if not exists ix_dpp_proj_dpp4fun_components_gin_5516a5a226f8b2f5 on dpp_projections using gin ((data -> 'components')) where projection_name = 'dpp4fun';
create index if not exists ix_dpp_proj_dpp4fun_parts_gin_17531e194439d03d on dpp_projections using gin ((data -> 'parts')) where projection_name = 'dpp4fun';
