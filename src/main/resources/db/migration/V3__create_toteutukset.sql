create sequence toteutus_oid;

create table toteutukset (
  oid varchar primary key default '1.2.246.562.17.' || lpad(nextval('toteutus_oid')::text, 20, '0'),
  koulutus_oid varchar not null references koulutukset (oid),
  tila julkaisutila not null default 'tallennettu',
  nimi jsonb not null,
  metadata jsonb,
  muokkaaja varchar not null,
  transaction_id bigint not null default txid_current(),
  system_time tstzrange not null default tstzrange(now(), null, '[)')
);

create table toteutukset_history (like toteutukset);

create trigger set_temporal_columns_on_toteutukset_on_insert
  before insert on toteutukset
  for each row
execute procedure set_temporal_columns();

create trigger set_temporal_columns_on_toteutukset_on_update
  before update on toteutukset
  for each row
execute procedure set_temporal_columns();

create or replace function update_toteutukset_history() returns trigger as
$$
begin
  insert into toteutukset_history (
      oid,
      koulutus_oid,
      tila,
      nimi,
      metadata,
      muokkaaja,
      transaction_id,
      system_time
      ) values (
                   old.oid,
                   old.koulutus_oid,
                   old.tila,
                   old.nimi,
                   old.metadata,
                   old.muokkaaja,
                   old.transaction_id,
                   tstzrange(lower(old.system_time), now(), '[)')
                   );
  return null;
end;
$$ language plpgsql;

create trigger toteutukset_history
  after update on toteutukset
  for each row
  when (old.transaction_id <> txid_current())
execute procedure update_toteutukset_history();

create trigger delete_toteutukset_history
  after delete on toteutukset
  for each row
execute procedure update_toteutukset_history();

