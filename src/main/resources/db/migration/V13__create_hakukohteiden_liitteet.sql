create table hakukohteiden_liitteet(
  id uuid primary key,
  hakukohde_oid varchar not null references hakukohteet(oid),
  tyyppi varchar,
  nimi jsonb,
  kuvaus jsonb,
  toimitusaika timestamp without time zone,
  toimitustapa liitteen_toimitustapa,
  toimitusosoite jsonb,
  muokkaaja varchar not null,
  transaction_id bigint not null default txid_current(),
  system_time tstzrange not null default tstzrange(now(), null, '[)')
);
alter table hakukohteiden_liitteet owner to oph;

create table hakukohteiden_liitteet_history (like hakukohteiden_liitteet);
alter table hakukohteiden_liitteet_history owner to oph;

create trigger set_temporal_columns_on_hakukohteiden_liitteet_on_insert
  before insert on hakukohteiden_liitteet
  for each row
execute procedure set_temporal_columns();

create trigger set_temporal_columns_on_hakukohteiden_liitteet_on_update
  before update on hakukohteiden_liitteet
  for each row
execute procedure set_temporal_columns();

create or replace function update_hakukohteiden_liitteet_history() returns trigger as
$$
begin
  insert into hakukohteiden_liitteet_history (
      id,
      hakukohde_oid,
      tyyppi,
      nimi,
      kuvaus,
      toimitusaika,
      toimitustapa,
      toimitusosoite,
      muokkaaja,
      transaction_id,
      system_time
      ) values (old.id,
                old.hakukohde_oid,
                old.tyyppi,
                old.nimi,
                old.kuvaus,
                old.toimitusaika,
                old.toimitustapa,
                old.toimitusosoite,
                old.muokkaaja,
                old.transaction_id,
                tstzrange(lower(old.system_time), now(), '[)'));
  return null;
end;
$$ language plpgsql;

create trigger hakukohteiden_liitteet_history
  after update on hakukohteiden_liitteet
  for each row
  when (old.transaction_id <> txid_current())
execute procedure update_hakukohteiden_liitteet_history();

create trigger delete_hakukohteiden_liitteet_history
  after delete on hakukohteiden_liitteet
  for each row
execute procedure update_hakukohteiden_liitteet_history();