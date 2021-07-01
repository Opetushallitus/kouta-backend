-- Haut
alter table haut
    add column external_id varchar;

alter table haut_history
    add column external_id varchar;

comment on column haut.external_id is 'Ulkoinen tunniste jota voidaan käyttää Kouta lomakkeiden mäppäykseen oppilaitosten omien tietojärjestelmien kanssa';

create or replace function update_haut_history() returns trigger
    language plpgsql
as
$$
begin
insert into haut_history (
    oid,
    external_id,
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
             old.external_id,
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
$$;



-- Hakukohteet
alter table hakukohteet
    add column external_id varchar;

alter table hakukohteet_history
    add column external_id varchar;

comment on column hakukohteet.external_id is 'Ulkoinen tunniste jota voidaan käyttää Kouta lomakkeiden mäppäykseen oppilaitosten omien tietojärjestelmien kanssa';

create or replace function update_hakukohteet_history() returns trigger
    language plpgsql
as
$$
begin
insert into hakukohteet_history (oid,
                                 external_id,
                                 toteutus_oid,
                                 haku_oid,
                                 tila,
                                 nimi,
                                 hakulomaketyyppi,
                                 hakulomake_ataru_id,
                                 hakulomake_kuvaus,
                                 hakulomake_linkki,
                                 kaytetaan_haun_hakulomaketta,
                                 jarjestyspaikka_oid,
                                 aloituspaikat,
                                 ensikertalaisen_aloituspaikat,
                                 pohjakoulutusvaatimus_koodi_urit,
                                 muu_pohjakoulutusvaatimus_kuvaus,
                                 pohjakoulutusvaatimus_tarkenne,
                                 toinen_aste_onko_kaksoistutkinto,
                                 kaytetaan_haun_aikataulua,
                                 valintaperuste_id,
                                 liitteet_onko_sama_toimitusaika,
                                 liitteet_onko_sama_toimitusosoite,
                                 liitteiden_toimitusaika,
                                 liitteiden_toimitustapa,
                                 liitteiden_toimitusosoite,
                                 esikatselu,
                                 metadata,
                                 muokkaaja,
                                 organisaatio_oid,
                                 kielivalinta,
                                 transaction_id,
                                 system_time)
values (old.oid,
        old.external_id,
        old.toteutus_oid,
        old.haku_oid,
        old.tila,
        old.nimi,
        old.hakulomaketyyppi,
        old.hakulomake_ataru_id,
        old.hakulomake_kuvaus,
        old.hakulomake_linkki,
        old.kaytetaan_haun_hakulomaketta,
        old.jarjestyspaikka_oid,
        old.aloituspaikat,
        old.ensikertalaisen_aloituspaikat,
        old.pohjakoulutusvaatimus_koodi_urit,
        old.muu_pohjakoulutusvaatimus_kuvaus,
        old.pohjakoulutusvaatimus_tarkenne,
        old.toinen_aste_onko_kaksoistutkinto,
        old.kaytetaan_haun_aikataulua,
        old.valintaperuste_id,
        old.liitteet_onko_sama_toimitusaika,
        old.liitteet_onko_sama_toimitusosoite,
        old.liitteiden_toimitusaika,
        old.liitteiden_toimitustapa,
        old.liitteiden_toimitusosoite,
        old.esikatselu,
        old.metadata,
        old.muokkaaja,
        old.organisaatio_oid,
        old.kielivalinta,
        old.transaction_id,
        tstzrange(lower(old.system_time), now(), '[)'));
return null;
end;
$$;
