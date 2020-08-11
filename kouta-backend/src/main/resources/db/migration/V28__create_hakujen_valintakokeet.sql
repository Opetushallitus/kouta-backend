create table hakujen_valintakokeet (
    id uuid primary key,
    haku_oid varchar not null references haut(oid),
    tyyppi varchar,
    tilaisuudet jsonb,
    muokkaaja varchar not null,
    transaction_id bigint not null default txid_current(),
    system_time tstzrange not null default tstzrange(now(), null, '[)')
);

create table hakujen_valintakokeet_history (like hakujen_valintakokeet);

create trigger set_temporal_columns_on_hakujen_valintakokeet_on_insert
    before insert on hakujen_valintakokeet
    for each row
execute procedure set_temporal_columns();

create trigger set_temporal_columns_on_hakujen_valintakokeet_on_update
    before update on hakujen_valintakokeet
    for each row
execute procedure set_temporal_columns();

create or replace function update_hakujen_valintakokeet_history() returns trigger as
$$
begin
    insert into hakujen_valintakokeet_history (
        id,
        haku_oid,
        tyyppi,
        tilaisuudet,
        muokkaaja,
        transaction_id,
        system_time
    ) values (old.id,
              old.haku_oid,
              old.tyyppi,
              old.tilaisuudet,
              old.muokkaaja,
              old.transaction_id,
              tstzrange(lower(old.system_time), now(), '[)'));
    return null;
end;
$$ language plpgsql;

create trigger hakujen_valintakokeet_history
    after update on hakujen_valintakokeet
    for each row
    when (old.transaction_id <> txid_current())
execute procedure update_hakujen_valintakokeet_history();

create trigger delete_hakujen_valintakokeet_history
    after delete on hakujen_valintakokeet
    for each row
execute procedure update_hakujen_valintakokeet_history();