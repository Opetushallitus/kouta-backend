create table sorakuvaukset (
    id uuid primary key,
    tila julkaisutila not null default 'tallennettu',
    nimi jsonb,
    koulutustyyppi koulutustyyppi not null,
    julkinen boolean not null default false,
    kielivalinta jsonb,
    metadata jsonb,
    organisaatio_oid varchar not null,
    muokkaaja varchar not null,
    transaction_id bigint not null default txid_current(),
    system_time tstzrange not null default tstzrange(now(), null, '[)')
);

create table sorakuvaukset_history (like sorakuvaukset);

create trigger set_temporal_columns_on_sorakuvaukset_on_insert
    before insert on sorakuvaukset
    for each row
execute procedure set_temporal_columns();

create trigger set_temporal_columns_on_sorakuvaukset_on_update
    before update on sorakuvaukset
    for each row
execute procedure set_temporal_columns();

create or replace function update_sorakuvaukset_history() returns trigger as
$$
begin
    insert into sorakuvaukset_history (
        id,
        tila,
        nimi,
        koulutustyyppi,
        julkinen,
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
        old.julkinen,
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

create trigger sorakuvaukset_history
    after update on sorakuvaukset
    for each row
    when (old.transaction_id <> txid_current())
execute procedure update_sorakuvaukset_history();

create trigger delete_sorakuvaukset_history
    after delete on sorakuvaukset
    for each row
execute procedure update_sorakuvaukset_history();