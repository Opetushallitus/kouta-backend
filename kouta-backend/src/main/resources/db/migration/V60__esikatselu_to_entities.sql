-- Hakukohteet
alter table hakukohteet
    add column esikatselu boolean not null default false;
alter table hakukohteet_history
    add column esikatselu boolean not null default false;

comment on column hakukohteet.esikatselu is 'Onko hakukohde nähtävissä esikatselussa';

create or replace function update_hakukohteet_history() returns trigger
    language plpgsql
as
$$
begin
    insert into hakukohteet_history (oid,
                                     toteutus_oid,
                                     haku_oid,
                                     tila,
                                     nimi,
                                     alkamiskausi_koodi_uri,
                                     alkamisvuosi,
                                     kaytetaan_haun_alkamiskautta,
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
            old.toteutus_oid,
            old.haku_oid,
            old.tila,
            old.nimi,
            old.alkamiskausi_koodi_uri,
            old.alkamisvuosi,
            old.kaytetaan_haun_alkamiskautta,
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

-- Koulutukset
alter table koulutukset
    add column esikatselu boolean not null default false;
alter table koulutukset_history
    add column esikatselu boolean not null default false;

comment on column koulutukset.esikatselu is 'Onko koulutus nähtävissä esikatselussa';

create or replace function update_koulutukset_history() returns trigger
    language plpgsql
as
$$
begin
    insert into koulutukset_history (oid,
                                     johtaa_tutkintoon,
                                     tyyppi,
                                     koulutus_koodi_uri,
                                     tila,
                                     nimi,
                                     metadata,
                                     muokkaaja,
                                     transaction_id,
                                     system_time,
                                     kielivalinta,
                                     organisaatio_oid,
                                     esikatselu,
                                     julkinen,
                                     teemakuva,
                                     eperuste_id)
    values (old.oid,
            old.johtaa_tutkintoon,
            old.tyyppi,
            old.koulutus_koodi_uri,
            old.tila,
            old.nimi,
            old.metadata,
            old.muokkaaja,
            old.transaction_id,
            tstzrange(lower(old.system_time), now(), '[)'),
            old.kielivalinta,
            old.organisaatio_oid,
            old.esikatselu,
            old.julkinen,
            old.teemakuva,
            old.eperuste_id);
    return null;
end;
$$;

-- Oppilaitokset
alter table oppilaitokset
    add column esikatselu boolean not null default false;
alter table oppilaitokset_history
    add column esikatselu boolean not null default false;

comment on column oppilaitokset.esikatselu is 'Onko oppilaitos nähtävissä esikatselussa';

create or replace function update_oppilaitokset_history() returns trigger
    language plpgsql
as
$$
begin
    insert into oppilaitokset_history (oid,
                                       tila,
                                       kielivalinta,
                                       metadata,
                                       organisaatio_oid,
                                       muokkaaja,
                                       esikatselu,
                                       transaction_id,
                                       system_time,
                                       teemakuva,
                                       logo)
    values (old.oid,
            old.tila,
            old.kielivalinta,
            old.metadata,
            old.organisaatio_oid,
            old.muokkaaja,
            old.esikatselu,
            old.transaction_id,
            tstzrange(lower(old.system_time), now(), '[)'),
            old.teemakuva,
            old.logo);
    return null;
end;
$$;

-- Oppilaitosten osat
alter table oppilaitosten_osat
    add column esikatselu boolean not null default false;
alter table oppilaitosten_osat_history
    add column esikatselu boolean not null default false;

comment on column oppilaitosten_osat.esikatselu is 'Onko oppilaitoksen osa nähtävissä esikatselussa';

create or replace function update_oppilaitosten_osat_history() returns trigger
    language plpgsql
as
$$
begin
    insert into oppilaitosten_osat_history (oid,
                                            oppilaitos_oid,
                                            tila,
                                            kielivalinta,
                                            metadata,
                                            organisaatio_oid,
                                            muokkaaja,
                                            esikatselu,
                                            transaction_id,
                                            system_time,
                                            teemakuva)
    values (old.oid,
            old.oppilaitos_oid,
            old.tila,
            old.kielivalinta,
            old.metadata,
            old.organisaatio_oid,
            old.muokkaaja,
            old.esikatselu,
            old.transaction_id,
            tstzrange(lower(old.system_time), now(), '[)'),
            old.teemakuva);
    return null;
end;
$$;
