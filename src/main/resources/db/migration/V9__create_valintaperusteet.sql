create table valintaperusteet(
  id uuid primary key,
  tila julkaisutila not null default 'tallennettu',
  hakutapa_koodi_uri varchar,
  kohdejoukko_koodi_uri varchar,
  kohdejoukon_tarkenne_koodi_uri varchar,
  nimi jsonb,
  kielivalinta jsonb,
  onkoJulkinen boolean not null default false,
  metadata jsonb,
  organisaatio varchar not null,
  muokkaaja varchar not null,
  transaction_id bigint not null default txid_current(),
  system_time tstzrange not null default tstzrange(now(), null, '[)')
);

create table valintaperusteet_history (like valintaperusteet);

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
      hakutapa_koodi_uri,
      kohdejoukko_koodi_uri,
      kohdejoukon_tarkenne_koodi_uri,
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
                   old.hakutapa_koodi_uri,
                   old.kohdejoukko_koodi_uri,
                   old.kohdejoukon_tarkenne_koodi_uri,
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