create table oppilaitokset(
    oid varchar primary key,
    tila julkaisutila not null default 'tallennettu',
    kielivalinta jsonb,
    metadata jsonb,
    organisaatio_oid varchar not null,
    muokkaaja varchar not null,
    transaction_id bigint not null default txid_current(),
    system_time tstzrange not null default tstzrange(now(), null, '[)')
);

create table oppilaitokset_history (like oppilaitokset);

create table oppilaitosten_osat(
    oid varchar primary key,
    oppilaitos_oid varchar not null references oppilaitokset(oid),
    tila julkaisutila not null default 'tallennettu',
    kielivalinta jsonb,
    metadata jsonb,
    organisaatio_oid varchar not null,
    muokkaaja varchar not null,
    transaction_id bigint not null default txid_current(),
    system_time tstzrange not null default tstzrange(now(), null, '[)')
);

create table oppilaitosten_osat_history (like oppilaitosten_osat);

create trigger set_temporal_columns_on_oppilaitokset_on_insert
    before insert on oppilaitokset
    for each row
execute procedure set_temporal_columns();

create trigger set_temporal_columns_on_oppilaitokset_on_update
    before update on oppilaitokset
    for each row
execute procedure set_temporal_columns();

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
        system_time
    ) values (
        old.oid,
        old.tila,
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

create trigger oppilaitokset_history
    after update on oppilaitokset
    for each row
    when (old.transaction_id <> txid_current())
execute procedure update_oppilaitokset_history();

create trigger delete_oppilaitokset_history
    after delete on oppilaitokset
    for each row
execute procedure update_oppilaitokset_history();

create trigger set_temporal_columns_on_oppilaitosten_osat_on_insert
    before insert on oppilaitosten_osat
    for each row
execute procedure set_temporal_columns();

create trigger set_temporal_columns_on_oppilaitosten_osat_on_update
    before update on oppilaitosten_osat
    for each row
execute procedure set_temporal_columns();

create or replace function update_oppilaitosten_osat_history() returns trigger as
$$
begin
    insert into oppilaitosten_osat_history (
        oid,
        oppilaitos_oid,
        tila,
        kielivalinta,
        metadata,
        organisaatio_oid,
        muokkaaja,
        transaction_id,
        system_time
    ) values (
        old.oid,
        old.oppilaitos_oid,
        old.tila,
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

create trigger oppilaitosten_osat_history
    after update on oppilaitosten_osat
    for each row
    when (old.transaction_id <> txid_current())
execute procedure update_oppilaitosten_osat_history();

create trigger delete_oppilaitosten_osat_history
    after delete on oppilaitosten_osat
    for each row
execute procedure update_oppilaitosten_osat_history();