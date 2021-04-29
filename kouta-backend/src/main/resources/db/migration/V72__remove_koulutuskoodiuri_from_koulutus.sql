alter table koulutukset drop column koulutus_koodi_uri;
alter table koulutukset_history drop column koulutus_koodi_uri;

create or replace function update_koulutukset_history() returns trigger
    language plpgsql
as
$$
begin
    insert into koulutukset_history (oid,
                                     johtaa_tutkintoon,
                                     tyyppi,
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
