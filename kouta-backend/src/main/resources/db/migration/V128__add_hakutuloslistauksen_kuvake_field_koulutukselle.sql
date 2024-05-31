alter table koulutukset add column hakutuloslistauksen_kuvake varchar;

comment on column koulutukset.hakutuloslistauksen_kuvake is 'URL hakutuloslistauksen kuvakkeeseen';

alter table koulutukset_history
    add column hakutuloslistauksen_kuvake varchar;

create or replace function update_koulutukset_history() returns trigger as
$$
begin
insert into koulutukset_history (
    oid,
    external_id,
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
    eperuste_id,
    sorakuvaus_id,
    last_modified,
    hakutuloslistauksen_kuvake
) values (
             old.oid,
             old.external_id,
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
             old.eperuste_id,
             old.sorakuvaus_id,
             old.last_modified,
             old.hakutuloslistauksen_kuvake
         );
return null;
end;
$$ language plpgsql;
