alter table migration_old_to_new_oid_lookup
add column update_allowed boolean not null default true;