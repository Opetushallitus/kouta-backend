create type hakulomaketyyppi as enum (
  'ataru',
  'haku-app',
  'muu',
  'ei sähköistä'
);
alter type hakulomaketyyppi owner to oph;

create sequence haku_oid;
alter sequence haku_oid owner to oph;

create table haut (
  oid varchar primary key default '1.2.246.562.29.' || lpad(nextval('haku_oid')::text, 20, '0'),
  tila julkaisutila not null default 'tallennettu',
  nimi jsonb,
  hakutapa_koodi_uri varchar,
  hakukohteen_liittamisen_takaraja timestamp without time zone,
  hakukohteen_muokkaamisen_takaraja timestamp without time zone,
  alkamiskausi_koodi_uri varchar,
  alkamisvuosi varchar(4),
  kohdejoukko_koodi_uri varchar,
  kohdejoukon_tarkenne_koodi_uri varchar,
  hakulomaketyyppi hakulomaketyyppi,
  hakulomake varchar,
  metadata jsonb,
  organisaatio varchar not null,
  muokkaaja varchar not null,
  transaction_id bigint not null default txid_current(),
  system_time tstzrange not null default tstzrange(now(), null, '[)')
);
alter table haut owner to oph;

create table haut_history (like haut);
alter table haut_history owner to oph;

create trigger set_temporal_columns_on_haut_on_insert
  before insert on haut
  for each row
execute procedure set_temporal_columns();

create trigger set_temporal_columns_on_haut_on_update
  before update on haut
  for each row
execute procedure set_temporal_columns();

create or replace function update_haut_history() returns trigger as
$$
begin
  insert into haut_history (
      oid,
      tila,
      nimi,
      hakutapa_koodi_uri,
      hakukohteen_liittamisen_takaraja,
      hakukohteen_muokkaamisen_takaraja,
      alkamiskausi_koodi_uri,
      alkamisvuosi,
      kohdejoukko_koodi_uri,
      kohdejoukon_tarkenne,
      hakulomaketyyppi,
      hakulomake,
      organisaatio,
      metadata,
      muokkaaja,
      transaction_id,
      system_time
      ) values (
                   old.oid,
                   old.tila,
                   old.nimi,
                   old.hakutapa_koodi_uri,
                   old.hakukohteen_liittamisen_takaraja,
                   old.hakukohteen_muokkaamisen_takaraja,
                   old.alkamiskausi_koodi_uri,
                   old.alkamisvuosi,
                   old.kohdejoukko_koodi_uri,
                   old.kohdejoukon_tarkenne,
                   old.hakulomaketyyppi,
                   old.hakulomake,
                   old.organisaatio,
                   old.metadata,
                   old.muokkaaja,
                   old.transaction_id,
                   tstzrange(lower(old.system_time), now(), '[)')
                   );
  return null;
end;
$$ language plpgsql;

create trigger haut_history
  after update on haut
  for each row
  when (old.transaction_id <> txid_current())
execute procedure update_haut_history();

create trigger delete_haut_history
  after delete on haut
  for each row
execute procedure update_haut_history();

