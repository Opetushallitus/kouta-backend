update haut
set metadata = jsonb_set(metadata, '{koulutuksenAlkamiskausi}', '{}', TRUE)
where (alkamiskausi_koodi_uri notnull or alkamisvuosi notnull);

update haut
set metadata = jsonb_set(metadata, '{koulutuksenAlkamiskausi,alkamiskausityyppi}', '"alkamiskausi ja -vuosi"', TRUE)
where (alkamiskausi_koodi_uri notnull or alkamisvuosi notnull);

update haut
set metadata = jsonb_set(metadata, '{koulutuksenAlkamiskausi,koulutuksenAlkamiskausiKoodiUri}', to_jsonb(alkamiskausi_koodi_uri), TRUE)
where alkamiskausi_koodi_uri notnull;

update haut
set metadata = jsonb_set(metadata, '{koulutuksenAlkamiskausi,koulutuksenAlkamisvuosi}', to_jsonb(alkamisvuosi), TRUE)
where alkamisvuosi notnull;

alter table haut drop column alkamiskausi_koodi_uri;
alter table haut_history drop column alkamiskausi_koodi_uri;

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