create table toteutusten_tarjoajat (
  toteutus_oid varchar references toteutukset (oid),
  tarjoaja_oid varchar not null,
  muokkaaja varchar not null,
  transaction_id bigint not null default txid_current(),
  system_time tstzrange not null default tstzrange(now(), null, '[)'),
  primary key (toteutus_oid, tarjoaja_oid)
);
alter table toteutusten_tarjoajat owner to oph;

create table toteutusten_tarjoajat_history (like toteutusten_tarjoajat);
alter table toteutusten_tarjoajat_history owner to oph;

create trigger set_temporal_columns_on_toteutusten_tarjoajat_on_insert
  before insert on toteutusten_tarjoajat
  for each row
execute procedure set_temporal_columns();

create trigger set_temporal_columns_on_toteutusten_tarjoajat_on_update
  before update on toteutusten_tarjoajat
  for each row
execute procedure set_temporal_columns();

create or replace function update_toteutusten_tarjoajat_history() returns trigger as
$$
begin
  insert into toteutusten_tarjoajat_history (
      toteutus_oid,
      tarjoaja_oid,
      muokkaaja,
      transaction_id,
      system_time
      ) values (
                   old.toteutus_oid,
                   old.tarjoaja_oid,
                   old.muokkaaja,
                   old.transaction_id,
                   tstzrange(lower(old.system_time), now(), '[)')
                );
  return null;
end;
$$ language plpgsql;

create trigger toteutusten_tarjoajat_history
  after update on toteutusten_tarjoajat
  for each row
  when (old.transaction_id <> txid_current())
execute procedure update_toteutusten_tarjoajat_history();

create trigger delete_toteutusten_tarjoajat_history
  after delete on toteutusten_tarjoajat
  for each row
execute procedure update_toteutusten_tarjoajat_history();