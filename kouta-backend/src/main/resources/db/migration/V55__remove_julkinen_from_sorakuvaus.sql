alter table sorakuvaukset drop column julkinen;
alter table sorakuvaukset_history drop column julkinen;

create or replace function update_sorakuvaukset_history() returns trigger as
$$
begin
    insert into sorakuvaukset_history (
        id,
        tila,
        nimi,
        koulutustyyppi,
        kielivalinta,
        metadata,
        organisaatio_oid,
        muokkaaja,
        transaction_id,
        system_time
    ) values (
        old.id,
        old.tila,
        old.nimi,
        old.koulutustyyppi,
        old.kielivalinta,
        old.metadata,
        old.organisaatio_oid,
        old.muokkaaja,
        old.transaction_id,
        tstzrange(lower(old.system_time), now(), '[)')
    );
    return null;
end;
$$ language plpgsql;