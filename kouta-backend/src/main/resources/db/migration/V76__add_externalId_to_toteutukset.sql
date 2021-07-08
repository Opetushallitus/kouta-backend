alter table toteutukset
    add column external_id varchar;

alter table toteutukset_history
    add column external_id varchar;

comment on column toteutukset.external_id is 'Ulkoinen tunniste jota voidaan käyttää Kouta lomakkeiden mäppäykseen oppilaitosten omien tietojärjestelmien kanssa';

create or replace function update_toteutukset_history() returns trigger
    language plpgsql
as
$$
begin
insert into toteutukset_history (oid,
                                 external_id,
                                 koulutus_oid,
                                 tila,
                                 nimi,
                                 metadata,
                                 muokkaaja,
                                 transaction_id,
                                 system_time,
                                 kielivalinta,
                                 esikatselu,
                                 organisaatio_oid,
                                 teemakuva,
                                 sorakuvaus_id)
values (old.oid,
        old.external_id,
        old.koulutus_oid,
        old.tila,
        old.nimi,
        old.metadata,
        old.muokkaaja,
        old.transaction_id,
        tstzrange(lower(old.system_time), now(), '[)'),
        old.kielivalinta,
        old.esikatselu,
        old.organisaatio_oid,
        old.teemakuva,
        old.sorakuvaus_id);
return null;
end;
$$;
