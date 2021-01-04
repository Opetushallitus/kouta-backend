alter table toteutukset
  add column sorakuvaus_id uuid references sorakuvaukset (id);

COMMENT ON COLUMN toteutukset.sorakuvaus_id IS 'Tutkintoon johtamattomaan toteutukseen liittyvän SORA-kuvauksen yksilöivä tunniste';

alter table toteutukset_history
    add column sorakuvaus_id uuid references sorakuvaukset (id);

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
        teemakuva,
        sorakuvaus_id
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
                 old.teemakuva,
                 old.sorakuvaus_id
             );
    return null;
end;
$$ language plpgsql;