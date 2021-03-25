alter table koulutukset add column if not exists koulutukset_koodi_uri varchar[];
alter table koulutukset_history add column if not exists koulutukset_koodi_uri varchar[];

comment on column koulutukset.koulutukset_koodi_uri is 'Koulutusten koodi URIt';

create or replace function update_koulutukset_history() returns trigger
    language plpgsql
as
$$
begin
    insert into koulutukset_history (oid,
                                     johtaa_tutkintoon,
                                     tyyppi,
                                     koulutus_koodi_uri,
                                     koulutukset_koodi_uri,
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
            old.koulutukset_koodi_uri,
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

-- Kopioidaan arvot vanhasta kentästä joka tullaan poistamaan myöhemmin
update koulutukset
set koulutukset_koodi_uri = Array [koulutus_koodi_uri]
where koulutukset_koodi_uri is null and koulutus_koodi_uri is not null;
