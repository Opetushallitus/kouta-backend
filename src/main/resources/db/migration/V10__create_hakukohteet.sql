create type liitteen_toimitustapa as enum (
  'lomake',
  'hakijapalvelu',
  'osoite'
);
alter type liitteen_toimitustapa owner to oph;

create table hakukohteet (
  oid varchar primary key default '1.2.246.562.20.' || lpad(nextval('haku_oid')::text, 20, '0'),
  toteutus_oid varchar not null references toteutukset(oid),
  haku_oid varchar not null references haut(oid),
  tila julkaisutila not null default 'tallennettu',
  nimi jsonb,
  alkamiskausi_koodi_uri varchar,
  alkamisvuosi varchar(4),
  hakulomaketyyppi hakulomaketyyppi,
  hakulomake varchar,
  aloituspaikat bigint,
  ensikertalaisen_aloituspaikat bigint,
  pohjakoulutusvaatimus_koodi_uri varchar,
  muu_pohjakoulutusvaatimus_kuvaus jsonb,
  toinen_aste_onko_kaksoistutkinto boolean,
  kaytetaan_haun_aikataulua boolean,
  valintaperuste uuid references valintaperusteet (id),
  liitteet_onko_sama_toimitusaika boolean,
  liitteet_onko_sama_toimitusosoite boolean,
  liitteiden_toimitusaika timestamp without time zone,
  liitteiden_toimitustapa liitteen_toimitustapa,
  liitteiden_toimitusosoite jsonb,
  metadata jsonb,
  muokkaaja varchar not null,
  kielivalinta jsonb,
  transaction_id bigint not null default txid_current(),
  system_time tstzrange not null default tstzrange(now(), null, '[)')
);
alter table hakukohteet owner to oph;

create table hakukohteet_history (like hakukohteet);
alter table hakukohteet_history owner to oph;

create trigger set_temporal_columns_on_hakukohteet_on_insert
  before insert on hakukohteet
  for each row
execute procedure set_temporal_columns();

create trigger set_temporal_columns_on_hakukohteet_on_update
  before update on hakukohteet
  for each row
execute procedure set_temporal_columns();

create or replace function update_hakukohteet_history() returns trigger as
$$
begin
  insert into hakukohteet_history (
      oid,
      toteutus_oid,
      haku_oid,
      tila,
      nimi,
      alkamiskausi_koodi_uri,
      alkamisvuosi,
      hakulomaketyyppi,
      hakulomake,
      aloituspaikat,
      ensikertalaisen_aloituspaikat,
      pohjakoulutusvaatimus_koodi_uri,
      muu_pohjakoulutusvaatimus_kuvaus,
      toinen_aste_onko_kaksoistutkinto,
      kaytetaan_haun_aikataulua,
      valintaperuste,
      liitteet_onko_sama_toimitusaika,
      liitteet_onko_sama_toimitusosoite,
      liitteiden_toimitusaika,
      liitteiden_toimitustapa,
      liitteiden_toimitusosoite,
      metadata,
      muokkaaja,
      kielivalinta,
      transaction_id,
      system_time
      ) values (
                 old.oid,
                 old.toteutus_oid,
                 old.haku_oid,
                 old.tila,
                 old.nimi,
                 old.alkamiskausi_koodi_uri,
                 old.alkamisvuosi,
                 old.hakulomaketyyppi,
                 old.hakulomake,
                 old.aloituspaikat,
                 old.ensikertalaisen_aloituspaikat,
                 old.pohjakoulutusvaatimus_koodi_uri,
                 old.muu_pohjakoulutusvaatimus_kuvaus,
                 old.toinen_aste_onko_kaksoistutkinto,
                 old.kaytetaan_haun_aikataulua,
                 old.valintaperuste,
                 old.liitteet_onko_sama_toimitusaika,
                 old.liitteet_onko_sama_toimitusosoite,
                 old.liitteiden_toimitusaika,
                 old.liitteiden_toimitustapa,
                 old.liitteiden_toimitusosoite,
                 old.metadata,
                 old.muokkaaja,
                 old.kielivalinta,
                 old.transaction_id,
                 tstzrange(lower(old.system_time), now(), '[)'));
  return null;
end;
$$ language plpgsql;

create trigger hakukohteet_history
  after update on hakukohteet
  for each row
  when (old.transaction_id <> txid_current())
execute procedure update_hakukohteet_history();

create trigger delete_hakukohteet_history
  after delete on hakukohteet
  for each row
execute procedure update_hakukohteet_history();