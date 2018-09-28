create table koulutusten_tekstit (
  koulutus_oid varchar references koulutukset (oid),
  kielikoodi character(2) not null,
  nimi varchar not null,
  kuvaus varchar,
  muokkaaja varchar not null,
  transaction_id bigint not null default txid_current(),
  system_time tstzrange not null default tstzrange(now(), null, '[)'),
  primary key (koulutus_oid, kielikoodi)
);
alter table koulutusten_tekstit owner to oph;

create table koulutusten_tekstit_history (like koulutusten_tekstit);
alter table koulutusten_tekstit_history owner to oph;

create trigger set_temporal_columns_on_koulutusten_tekstit_on_insert
  before insert on koulutusten_tekstit
  for each row
execute procedure set_temporal_columns();

create trigger set_temporal_columns_on_koulutusten_tekstit_on_update
  before update on koulutusten_tekstit
  for each row
execute procedure set_temporal_columns();

create or replace function update_koulutusten_tekstit_history() returns trigger as
$$
begin
  insert into koulutusten_tekstit_history (
      koulutus_oid,
      kielikoodi,
      nimi,
      kuvaus,
      muokkaaja,
      transaction_id,
      system_time
      ) values (
                   old.koulutus_oid,
                   old.kielikoodi,
                   old.nimi,
                   old.kuvaus,
                   old.muokkaaja,
                   old.transaction_id,
                   tstzrange(lower(old.system_time), now(), '[)')
                );
  return null;
end;
$$ language plpgsql;

create trigger koulutusten_tekstit_history
  after update on koulutusten_tekstit
  for each row
  when (old.transaction_id <> txid_current())
execute procedure update_koulutusten_tekstit_history();

create trigger delete_koulutusten_tekstit_history
  after delete on koulutusten_tekstit
  for each row
execute procedure update_koulutusten_tekstit_history();