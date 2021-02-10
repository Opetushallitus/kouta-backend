create table if not exists migration_old_to_new_oid_lookup (
  old_oid varchar primary key,
  new_oid varchar not null
);
