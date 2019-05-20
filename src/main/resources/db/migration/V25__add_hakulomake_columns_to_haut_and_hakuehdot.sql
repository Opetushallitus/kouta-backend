alter table haut
  rename column hakulomake to hakulomake_kuvaus;
alter table haut
  add column hakulomake_ataru_id uuid,
  add column hakulomake_linkki jsonb;

alter table haut_history
  rename column hakulomake to hakulomake_kuvaus;
alter table haut_history
  add column hakulomake_ataru_id uuid,
  add column hakulomake_linkki jsonb;

alter table hakukohteet
  rename column hakulomake to hakulomake_kuvaus;
alter table hakukohteet
  add column hakulomake_ataru_id uuid,
  add column hakulomake_linkki jsonb,
  add column eri_hakulomake_kuin_haulla boolean;

alter table hakukohteet_history
  rename column hakulomake to hakulomake_kuvaus;
alter table hakukohteet_history
  add column hakulomake_ataru_id uuid,
  add column hakulomake_linkki jsonb,
  add column eri_hakulomake_kuin_haulla boolean;

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
    ajastettu_julkaisu,
    alkamiskausi_koodi_uri,
    alkamisvuosi,
    kohdejoukko_koodi_uri,
    kohdejoukon_tarkenne_koodi_uri,
    hakulomaketyyppi,
    hakulomake_ataru_id,
    hakulomake_kuvaus,
    hakulomake_linkki,
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
             old.ajastettu_julkaisu,
             old.alkamiskausi_koodi_uri,
             old.alkamisvuosi,
             old.kohdejoukko_koodi_uri,
             old.kohdejoukon_tarkenne_koodi_uri,
             old.hakulomaketyyppi,
             old.hakulomake_ataru_id,
             old.hakulomake_kuvaus,
             old.hakulomake_linkki,
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
    hakulomake_ataru_id,
    hakulomake_kuvaus,
    hakulomake_linkki,
    eri_hakulomake_kuin_haulla,
    aloituspaikat,
    ensikertalaisen_aloituspaikat,
    pohjakoulutusvaatimus_koodi_urit,
    muu_pohjakoulutusvaatimus_kuvaus,
    toinen_aste_onko_kaksoistutkinto,
    kaytetaan_haun_aikataulua,
    valintaperuste_id,
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
             old.hakulomake_ataru_id,
             old.hakulomake_kuvaus,
             old.hakulomake_linkki,
             old.eri_hakulomake_kuin_haulla,
             old.aloituspaikat,
             old.ensikertalaisen_aloituspaikat,
             old.pohjakoulutusvaatimus_koodi_urit,
             old.muu_pohjakoulutusvaatimus_kuvaus,
             old.toinen_aste_onko_kaksoistutkinto,
             old.kaytetaan_haun_aikataulua,
             old.valintaperuste_id,
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
