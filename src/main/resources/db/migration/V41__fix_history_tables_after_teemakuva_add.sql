-- Koulutukset

alter table koulutukset_history add column teemakuva varchar;

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
        transaction_id,
        system_time,
        kielivalinta,
        organisaatio_oid,
        julkinen,
        teemakuva
    ) values (
                 old.oid,
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
                 old.julkinen,
                 old.teemakuva
             );
    return null;
end;
$$ language plpgsql;

-- Toteutukset

alter table toteutukset_history
    add column teemakuva varchar;

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
        transaction_id,
        system_time,
        kielivalinta,
        organisaatio_oid,
        teemakuva
    ) values (
                 old.oid,
                 old.koulutus_oid,
                 old.tila,
                 old.nimi,
                 old.metadata,
                 old.muokkaaja,
                 old.transaction_id,
                 tstzrange(lower(old.system_time), now(), '[)'),
                 old.kielivalinta,
                 old.organisaatio_oid,
                 old.teemakuva
             );
    return null;
end;
$$ language plpgsql;

-- Oppilaitokset

alter table oppilaitokset_history
    add column teemakuva varchar;

create or replace function update_oppilaitokset_history() returns trigger as
$$
begin
    insert into oppilaitokset_history (
        oid,
        tila,
        kielivalinta,
        metadata,
        organisaatio_oid,
        muokkaaja,
        transaction_id,
        system_time,
        teemakuva
    ) values (
                 old.oid,
                 old.tila,
                 old.kielivalinta,
                 old.metadata,
                 old.organisaatio_oid,
                 old.muokkaaja,
                 old.transaction_id,
                 tstzrange(lower(old.system_time), now(), '[)'),
                 old.teemakuva
             );
    return null;
end;
$$ language plpgsql;

-- Oppilaitosten osat

alter table oppilaitosten_osat_history
    add column teemakuva varchar;

create or replace function update_oppilaitosten_osat_history() returns trigger as
$$
begin
    insert into oppilaitosten_osat_history (
        oid,
        oppilaitos_oid,
        tila,
        kielivalinta,
        metadata,
        organisaatio_oid,
        muokkaaja,
        transaction_id,
        system_time,
        teemakuva
    ) values (
                 old.oid,
                 old.oppilaitos_oid,
                 old.tila,
                 old.kielivalinta,
                 old.metadata,
                 old.organisaatio_oid,
                 old.muokkaaja,
                 old.transaction_id,
                 tstzrange(lower(old.system_time), now(), '[)'),
                 old.teemakuva
             );
    return null;
end;
$$ language plpgsql;
