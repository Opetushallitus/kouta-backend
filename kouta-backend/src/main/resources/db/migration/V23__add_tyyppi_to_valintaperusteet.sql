alter table valintaperusteet
  add column koulutustyyppi koulutustyyppi not null default 'amm';

alter table valintaperusteet_history
  add column koulutustyyppi koulutustyyppi not null default 'amm';

alter table valintaperusteet
  alter column koulutustyyppi drop default;

alter table valintaperusteet_history
  alter column koulutustyyppi drop default;

create or replace function update_valintaperusteet_history() returns trigger as
$$
begin
  insert into valintaperusteet_history (
    koulutustyyppi,
    id,
    tila,
    nimi,
    hakutapa_koodi_uri,
    kohdejoukko_koodi_uri,
    kohdejoukon_tarkenne_koodi_uri,
    organisaatio_oid,
    metadata,
    kielivalinta,
    onkoJulkinen,
    muokkaaja,
    transaction_id,
    system_time
  ) values (
             old.koulutustyyppi,
             old.id,
             old.tila,
             old.nimi,
             old.hakutapa_koodi_uri,
             old.kohdejoukko_koodi_uri,
             old.kohdejoukon_tarkenne_koodi_uri,
             old.organisaatio_oid,
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
