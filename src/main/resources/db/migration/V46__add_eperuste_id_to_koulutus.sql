
alter table koulutukset
    add column eperuste_id bigint;

comment on column koulutukset.teemakuva is 'Koulutuksen käyttämän ePerusteen id';

alter table koulutukset_history
    add column eperuste_id bigint;

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
        teemakuva,
        eperuste_id
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
                 old.teemakuva,
                 old.eperuste_id
             );
    return null;
end;
$$ language plpgsql;


