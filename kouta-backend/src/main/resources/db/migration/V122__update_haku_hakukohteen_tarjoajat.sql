alter table haut
    add column if not exists hakukohteen_liittaja_organisaatiot varchar(127) [] not null default '{}';

alter table haut_history
    add column if not exists hakukohteen_liittaja_organisaatiot varchar(127) [] not null default '{}';

comment on column haut.hakukohteen_liittaja_organisaatiot is 'Organisaatiot jotka voivat liittää hakuun hakukohteita';

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
                          hakukohteen_liittaja_organisaatiot,
                          ajastettu_julkaisu,
                          ajastettu_haun_ja_hakukohteiden_arkistointi,
                          ajastettu_haun_ja_hakukohteiden_arkistointi_ajettu,
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
                          system_time,
                          last_modified)
values (old.oid,
        old.external_id,
        old.tila,
        old.nimi,
        old.hakutapa_koodi_uri,
        old.hakukohteen_liittamisen_takaraja,
        old.hakukohteen_muokkaamisen_takaraja,
        old.hakukohteen_liittaja_organisaatiot,
        old.ajastettu_julkaisu,
        old.ajastettu_haun_ja_hakukohteiden_arkistointi,
        old.ajastettu_haun_ja_hakukohteiden_arkistointi_ajettu,
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
        tstzrange(lower(old.system_time), now(), '[)'),
        old.last_modified);
return null;
end;
$$ language plpgsql;