alter table koulutukset
    add column external_id varchar;

alter table koulutukset_history
    add column external_id varchar;

comment on column koulutukset.external_id is 'Ulkoinen tunniste jota voidaan käyttää Kouta lomakkeiden mäppäykseen oppilaitosten omien tietojärjestelmien kanssa';

create or replace function update_koulutukset_history() returns trigger
    language plpgsql
as
$$
begin
insert into koulutukset_history (oid,
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
                                 eperuste_id)
values (old.oid,
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
        old.eperuste_id);
return null;
end;
$$;
