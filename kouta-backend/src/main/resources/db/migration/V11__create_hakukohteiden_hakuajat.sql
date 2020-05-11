create table hakukohteiden_hakuajat (
  hakukohde_oid varchar not null references hakukohteet(oid),
  hakuaika tsrange not null,
  muokkaaja varchar not null,
  transaction_id bigint not null default txid_current(),
  system_time tstzrange not null default tstzrange(now(), null, '[)'),
  primary key (hakukohde_oid, hakuaika)
);

create table hakukohteiden_hakuajat_history (like hakukohteiden_hakuajat);

create trigger set_temporal_columns_on_hakukohteiden_hakuajat_on_insert
  before insert on hakukohteiden_hakuajat
  for each row
execute procedure set_temporal_columns();

create trigger set_temporal_columns_on_hakukohteiden_hakuajat_on_update
  before update on hakukohteiden_hakuajat
  for each row
execute procedure set_temporal_columns();

create or replace function update_hakukohteiden_hakuajat_history() returns trigger as
$$
begin
  insert into hakukohteiden_hakuajat_history (
      hakukohde_oid,
      hakuaika,
      muokkaaja,
      transaction_id,
      system_time
      ) values (
                   old.hakukohde_oid,
                   old.hakuaika,
                   old.muokkaaja,
                   old.transaction_id,
                   tstzrange(lower(old.system_time), now(), '[)')
                   );
  return null;
end;
$$ language plpgsql;

create trigger hakukohteiden_hakuajat_history
  after update on hakukohteiden_hakuajat
  for each row
  when (old.transaction_id <> txid_current())
execute procedure update_hakukohteiden_hakuajat_history();

create trigger delete_hakukohteiden_hakuajat_history
  after delete on hakukohteiden_hakuajat
  for each row
execute procedure update_hakukohteiden_hakuajat_history();
