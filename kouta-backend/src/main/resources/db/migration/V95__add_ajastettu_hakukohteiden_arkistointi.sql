alter table haut
    add column ajastettu_hakukohteiden_arkistointi timestamp without time zone;
alter table haut_history
    add column ajastettu_hakukohteiden_arkistointi timestamp without time zone;

COMMENT ON COLUMN haut.ajastettu_hakukohteiden_arkistointi IS 'Ajanhetki, jolloin haku ja siihen liittyvät hakukohteet arkistoidaan automaattisesti Opintopolussa. Jos tyhjä, arkistoidaan automaattisesti 10 kk hakuajan päättymisen jälkeen.';

create or replace function update_haut_history() returns trigger as
$$
begin
    insert into haut_history (oid,
                              external_id,
                              tila,
                              nimi,
                              hakutapa_koodi_uri,
                              hakukohteen_liittamisen_takaraja,
                              hakukohteen_muokkaamisen_takaraja,
                              ajastettu_julkaisu,
                              ajastettu_hakukohteiden_arkistointi,
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
                              system_time)
    values (old.oid,
            old.external_id,
            old.tila,
            old.nimi,
            old.hakutapa_koodi_uri,
            old.hakukohteen_liittamisen_takaraja,
            old.hakukohteen_muokkaamisen_takaraja,
            old.ajastettu_julkaisu,
            old.ajastettu_hakukohteiden_arkistointi,
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
            tstzrange(lower(old.system_time), now(), '[)'));
    return null;
end;
$$ language plpgsql;