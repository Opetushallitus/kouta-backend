set client_encoding = 'UTF8';

create type koulutustyyppi as enum (
  'amm',
  'kk',
  'lk',
  'muu'
);

create type julkaisutila as enum (
  'tallennettu',
  'julkaistu',
  'arkistoitu'
);

create sequence koulutus_oid;

create table koulutukset (
  oid varchar primary key default '1.2.246.562.13.' || lpad(nextval('koulutus_oid')::text, 20, '0'),
  johtaa_tutkintoon boolean not null,
  tyyppi koulutustyyppi not null,
  koulutus_koodi_uri varchar not null,
  tila julkaisutila not null default 'tallennettu',
  nimi jsonb not null,
  metadata jsonb,
  muokkaaja varchar not null,
  transaction_id bigint not null default txid_current(),
  system_time tstzrange not null default tstzrange(now(), null, '[)')
);

create table koulutukset_history (like koulutukset);

create or replace function set_temporal_columns() returns trigger as
$$
begin
  new.system_time := tstzrange(now(), null, '[)');
  new.transaction_id := txid_current();
  return new;
end;
$$ language plpgsql;

create trigger set_temporal_columns_on_koulutukset_on_insert
  before insert on koulutukset
  for each row
execute procedure set_temporal_columns();

create trigger set_temporal_columns_on_koulutukset_on_update
  before update on koulutukset
  for each row
execute procedure set_temporal_columns();

create or replace function update_koulutukset_history() returns trigger as
$$
begin
  insert into koulutukset_history (
      oid,
      johtaa_tutkintoon,
      tyyppi,
      koulutus_koodi_uri,
      tila,
      nimi,
      metadata,
      muokkaaja,
      transaction_id,
      system_time
      ) values (
                   old.oid,
                   old.johtaa_tutkintoon,
                   old.tyyppi,
                   old.koulutus_koodi_uri,
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

create trigger koulutukset_history
  after update on koulutukset
  for each row
  when (old.transaction_id <> txid_current())
execute procedure update_koulutukset_history();

create trigger delete_koulutukset_history
  after delete on koulutukset
  for each row
execute procedure update_koulutukset_history();