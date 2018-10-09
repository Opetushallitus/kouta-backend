create table koulutusten_tarjoajat (
  koulutus_oid varchar references koulutukset (oid),
  tarjoaja_oid varchar not null,
  muokkaaja varchar not null,
  transaction_id bigint not null default txid_current(),
  system_time tstzrange not null default tstzrange(now(), null, '[)'),
  primary key (koulutus_oid, tarjoaja_oid)
);
alter table koulutusten_tarjoajat owner to oph;

create table koulutusten_tarjoajat_history (like koulutusten_tarjoajat);
alter table koulutusten_tarjoajat_history owner to oph;

create trigger set_temporal_columns_on_koulutusten_tarjoajat_on_insert
  before insert on koulutusten_tarjoajat
  for each row
execute procedure set_temporal_columns();

create trigger set_temporal_columns_on_koulutusten_tarjoajat_on_update
  before update on koulutusten_tarjoajat
  for each row
execute procedure set_temporal_columns();

create or replace function update_koulutusten_tarjoajat_history() returns trigger as
$$
begin
  insert into koulutusten_tarjoajat_history (
      koulutus_oid,
      tarjoaja_oid,
      muokkaaja,
      transaction_id,
      system_time
      ) values (
                   old.koulutus_oid,
                   old.tarjoaja_oid,
                   old.muokkaaja,
                   old.transaction_id,
                   tstzrange(lower(old.system_time), now(), '[)')
                );
  return null;
end;
$$ language plpgsql;

create trigger koulutusten_tarjoajat_history
  after update on koulutusten_tarjoajat
  for each row
  when (old.transaction_id <> txid_current())
execute procedure update_koulutusten_tarjoajat_history();

create trigger delete_koulutusten_tarjoajat_history
  after delete on koulutusten_tarjoajat
  for each row
execute procedure update_koulutusten_tarjoajat_history();