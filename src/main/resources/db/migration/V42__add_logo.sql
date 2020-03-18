-- Koulutukset

alter table oppilaitokset add column logo varchar;

comment on column oppilaitokset.logo is 'URL oppilaitoksen logoon';

alter table oppilaitokset_history
    add column logo varchar;

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
        teemakuva,
        logo
    ) values (
                 old.oid,
                 old.tila,
                 old.kielivalinta,
                 old.metadata,
                 old.organisaatio_oid,
                 old.muokkaaja,
                 old.transaction_id,
                 tstzrange(lower(old.system_time), now(), '[)'),
                 old.teemakuva,
                 old.logo
             );
    return null;
end;
$$ language plpgsql;
