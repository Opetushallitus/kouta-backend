create table hakukohteiden_valintakokeet (
  id uuid primary key,
  hakukohde_oid varchar not null references hakukohteet(oid),
  tyyppi varchar,
  tilaisuudet jsonb,
  muokkaaja varchar not null,
  transaction_id bigint not null default txid_current(),
  system_time tstzrange not null default tstzrange(now(), null, '[)')
);

create table hakukohteiden_valintakokeet_history (like hakukohteiden_valintakokeet);

create trigger set_temporal_columns_on_hakukohteiden_valintakokeet_on_insert
  before insert on hakukohteiden_valintakokeet
  for each row
execute procedure set_temporal_columns();

create trigger set_temporal_columns_on_hakukohteiden_valintakokeet_on_update
  before update on hakukohteiden_valintakokeet
  for each row
execute procedure set_temporal_columns();

create or replace function update_hakukohteiden_valintakokeet_history() returns trigger as
$$
begin
  insert into hakukohteiden_valintakokeet_history (
      id,
      hakukohde_oid,
      tyyppi,
      tilaisuudet,
      muokkaaja,
      transaction_id,
      system_time
      ) values (old.id,
                old.hakukohde_oid,
                old.tyyppi,
                old.tilaisuudet,
                old.muokkaaja,
                old.transaction_id,
                tstzrange(lower(old.system_time), now(), '[)'));
  return null;
end;
$$ language plpgsql;

create trigger hakukohteiden_valintakokeet_history
  after update on hakukohteiden_valintakokeet
  for each row
  when (old.transaction_id <> txid_current())
execute procedure update_hakukohteiden_valintakokeet_history();

create trigger delete_hakukohteiden_valintakokeet_history
  after delete on hakukohteiden_valintakokeet
  for each row
execute procedure update_hakukohteiden_valintakokeet_history();