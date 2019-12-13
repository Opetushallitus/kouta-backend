create table hakujen_hakuajat (
  haku_oid varchar not null references haut(oid),
  hakuaika tsrange not null,
  muokkaaja varchar not null,
  transaction_id bigint not null default txid_current(),
  system_time tstzrange not null default tstzrange(now(), null, '[)'),
  primary key (haku_oid, hakuaika)
);

create table hakujen_hakuajat_history (like hakujen_hakuajat);

create trigger set_temporal_columns_on_hakujen_hakuajat_on_insert
  before insert on hakujen_hakuajat
  for each row
execute procedure set_temporal_columns();

create trigger set_temporal_columns_on_hakujen_hakuajat_on_update
  before update on hakujen_hakuajat
  for each row
execute procedure set_temporal_columns();

create or replace function update_hakujen_hakuajat_history() returns trigger as
$$
begin
  insert into hakujen_hakuajat_history (
      haku_oid,
      hakuaika,
      muokkaaja,
      transaction_id,
      system_time
      ) values (
                   old.haku_oid,
                   old.hakuaika,
                   old.muokkaaja,
                   old.transaction_id,
                   tstzrange(lower(old.system_time), now(), '[)')
                   );
  return null;
end;
$$ language plpgsql;

create trigger hakujen_hakuajat_history
  after update on hakujen_hakuajat
  for each row
  when (old.transaction_id <> txid_current())
execute procedure update_hakujen_hakuajat_history();

create trigger delete_hakujen_hakuajat_history
  after delete on hakujen_hakuajat
  for each row
execute procedure update_hakujen_hakuajat_history();
