alter table koulutukset add column organisaatio_oid varchar not null default '1.2.246.562.10.81934895871';
alter table koulutukset_history add column organisaatio_oid varchar not null;
alter table toteutukset add column organisaatio_oid varchar not null;
alter table toteutukset_history add column organisaatio_oid varchar not null;
alter table hakukohteet add column organisaatio_oid varchar not null;
alter table hakukohteet_history add column organisaatio_oid varchar not null;

alter table koulutukset alter column organisaatio_oid drop default;

alter table haut rename column organisaatio to organisaatio_oid;
alter table haut_history rename column organisaatio to organisaatio_oid;
alter table valintaperusteet rename column organisaatio to organisaatio_oid;
alter table valintaperusteet_history rename column organisaatio to organisaatio_oid;

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
      organisaatio_oid,
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
                   old.organisaatio_oid,
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
      organisaatio_oid,
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
                   old.organisaatio_oid,
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
      hakutapa_koodi_uri,
      hakukohteen_liittamisen_takaraja,
      hakukohteen_muokkaamisen_takaraja,
      alkamiskausi_koodi_uri,
      alkamisvuosi,
      kohdejoukko_koodi_uri,
      kohdejoukon_tarkenne_koodi_uri,
      hakulomaketyyppi,
      hakulomake,
      metadata,
      muokkaaja,
      organisaatio_oid,
      kielivalinta,
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
                   old.kohdejoukon_tarkenne_koodi_uri,
                   old.hakulomaketyyppi,
                   old.hakulomake,
                   old.metadata,
                   old.muokkaaja,
                   old.organisaatio_oid,
                   old.kielivalinta,
                   old.transaction_id,
                   tstzrange(lower(old.system_time), now(), '[)')
                   );
  return null;
end;
$$ language plpgsql;

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
      organisaatio_oid,
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
                   old.organisaatio_oid,
                   old.kielivalinta,
                   old.transaction_id,
                   tstzrange(lower(old.system_time), now(), '[)'));
  return null;
end;
$$ language plpgsql;


create or replace function update_valintaperusteet_history() returns trigger as
$$
begin
  insert into valintaperusteet_history (
      id,
      tila,
      nimi,
      hakutapa_koodi_uri,
      kohdejoukko_koodi_uri,
      kohdejoukon_tarkenne_koodi_uri,
      metadata,
      kielivalinta,
      onkoJulkinen,
      muokkaaja,
      organisaatio_oid,
      transaction_id,
      system_time
      ) values (
                   old.id,
                   old.tila,
                   old.nimi,
                   old.hakutapa_koodi_uri,
                   old.kohdejoukko_koodi_uri,
                   old.kohdejoukon_tarkenne_koodi_uri,
                   old.metadata,
                   old.kielivalinta,
                   old.onkoJulkinen,
                   old.muokkaaja,
                   old.organisaatio_oid,
                   old.transaction_id,
                   tstzrange(lower(old.system_time), now(), '[)')
                   );
  return null;
end;
$$ language plpgsql;