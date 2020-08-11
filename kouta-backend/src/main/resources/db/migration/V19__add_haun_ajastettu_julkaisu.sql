alter table haut add column ajastettu_julkaisu timestamp without time zone;
alter table haut_history add column ajastettu_julkaisu timestamp without time zone;

create or replace function update_haut_history() returns trigger as
$$
begin
  insert into haut_history (
      oid,
      tila,
      nimi,
      hakutapa_koodi_uri,
      hakukohteen_liittamisen_takaraja,
      hakukohteen_muokkaamisen_takaraja,
      ajastettu_julkaisu,
      alkamiskausi_koodi_uri,
      alkamisvuosi,
      kohdejoukko_koodi_uri,
      kohdejoukon_tarkenne_koodi_uri,
      hakulomaketyyppi,
      hakulomake,
      metadata,
      muokkaaja,
      organisaatio_oid,
      kielivalinta,
      transaction_id,
      system_time
      ) values (
                   old.oid,
                   old.tila,
                   old.nimi,
                   old.hakutapa_koodi_uri,
                   old.hakukohteen_liittamisen_takaraja,
                   old.hakukohteen_muokkaamisen_takaraja,
                   old.ajastettu_julkaisu,
                   old.alkamiskausi_koodi_uri,
                   old.alkamisvuosi,
                   old.kohdejoukko_koodi_uri,
                   old.kohdejoukon_tarkenne_koodi_uri,
                   old.hakulomaketyyppi,
                   old.hakulomake,
                   old.metadata,
                   old.muokkaaja,
                   old.organisaatio_oid,
                   old.kielivalinta,
                   old.transaction_id,
                   tstzrange(lower(old.system_time), now(), '[)')
                   );
  return null;
end;
$$ language plpgsql;