alter table koulutukset add column kielivalinta jsonb;
alter table koulutukset_history add column kielivalinta jsonb;
alter table toteutukset add column kielivalinta jsonb;
alter table toteutukset_history add column kielivalinta jsonb;
alter table haut add column kielivalinta jsonb;
alter table haut_history add column kielivalinta jsonb;

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
      kielivalinta,
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
                   old.kielivalinta,
                   old.transaction_id,
                   tstzrange(lower(old.system_time), now(), '[)')
                   );
  return null;
end;
$$ language plpgsql;

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
      kielivalinta,
      transaction_id,
      system_time
      ) values (
                   old.oid,
                   old.koulutus_oid,
                   old.tila,
                   old.nimi,
                   old.metadata,
                   old.muokkaaja,
                   old.kielivalinta,
                   old.transaction_id,
                   tstzrange(lower(old.system_time), now(), '[)')
                   );
  return null;
end;
$$ language plpgsql;

create or replace function update_haut_history() returns trigger as
$$
begin
  insert into haut_history (
      oid,
      tila,
      nimi,
      hakutapa,
      hakukohteen_liittamisen_takaraja,
      hakukohteen_muokkaamisen_takaraja,
      alkamiskausi,
      alkamisvuosi,
      kohdejoukko,
      kohdejoukon_tarkenne,
      hakulomaketyyppi,
      hakulomake,
      organisaatio,
      metadata,
      muokkaaja,
      kielivalinta,
      transaction_id,
      system_time
      ) values (
                   old.oid,
                   old.tila,
                   old.nimi,
                   old.hakutapa,
                   old.hakukohteen_liittamisen_takaraja,
                   old.hakukohteen_muokkaamisen_takaraja,
                   old.alkamiskausi,
                   old.alkamisvuosi,
                   old.kohdejoukko,
                   old.kohdejoukon_tarkenne,
                   old.hakulomaketyyppi,
                   old.hakulomake,
                   old.organisaatio,
                   old.metadata,
                   old.muokkaaja,
                   old.kielivalinta,
                   old.transaction_id,
                   tstzrange(lower(old.system_time), now(), '[)')
                   );
  return null;
end;
$$ language plpgsql;