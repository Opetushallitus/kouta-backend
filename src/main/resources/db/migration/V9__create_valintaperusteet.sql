create type valintaperusteenkohde as enum (
  'kk yhteishaku',
  'kk siirtohaku',
  'amm',
  'lk'
  'muu kohde'
);
alter type valintaperusteenkohde owner to oph;

create table valintaperusteet(
  id uuid primary key,
  tila julkaisutila not null default 'tallennettu',
  kohde valintaperusteenkohde,
  nimi jsonb,
  kielivalinta jsonb,
  onkoJulkinen boolean not null default false,
  metadata jsonb,
  organisaatio varchar not null,
  muokkaaja varchar not null,
  transaction_id bigint not null default txid_current(),
  system_time tstzrange not null default tstzrange(now(), null, '[)')
);
alter table valintaperusteet owner to oph;

create table valintaperusteet_history (like valintaperusteet);
alter table valintaperusteet_history owner to oph;

create trigger set_temporal_columns_on_valintaperusteet_on_insert
  before insert on valintaperusteet
  for each row
execute procedure set_temporal_columns();

create trigger set_temporal_columns_on_valintaperusteet_on_update
  before update on valintaperusteet
  for each row
execute procedure set_temporal_columns();

create or replace function update_valintaperusteet_history() returns trigger as
$$
begin
  insert into valintaperusteet_history (
      id,
      tila,
      nimi,
      kohde,
      organisaatio,
      metadata,
      kielivalinta,
      onkoJulkinen,
      muokkaaja,
      transaction_id,
      system_time
      ) values (
                   old.id,
                   old.tila,
                   old.nimi,
                   old.kohde,
                   old.organisaatio,
                   old.metadata,
                   old.kielivalinta,
                   old.onkoJulkinen,
                   old.muokkaaja,
                   old.transaction_id,
                   tstzrange(lower(old.system_time), now(), '[)')
                   );
  return null;
end;
$$ language plpgsql;

create trigger valintaperusteet_history
  after update on valintaperusteet
  for each row
  when (old.transaction_id <> txid_current())
execute procedure update_valintaperusteet_history();

create trigger delete_valintaperusteet_history
  after delete on valintaperusteet
  for each row
execute procedure update_valintaperusteet_history();