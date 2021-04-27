alter table koulutukset
    add column sorakuvaus_id uuid references sorakuvaukset (id);

alter table koulutukset_history
    add column sorakuvaus_id uuid references sorakuvaukset (id);

comment on column koulutukset.sorakuvaus_id is 'Koulutukseen liittyvän SORA-kuvauksen yksilöivä tunniste';

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
                                     sorakuvaus_id,
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
            old.sorakuvaus_id,
            old.esikatselu,
            old.julkinen,
            old.teemakuva,
            old.eperuste_id);
    return null;
end;
$$;

