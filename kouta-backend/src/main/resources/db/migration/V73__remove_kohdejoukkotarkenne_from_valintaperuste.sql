alter table valintaperusteet drop column kohdejoukon_tarkenne_koodi_uri;
alter table valintaperusteet_history drop column kohdejoukon_tarkenne_koodi_uri;

create or replace function update_valintaperusteet_history() returns trigger
    language plpgsql
as
$$
begin
    insert into valintaperusteet_history (koulutustyyppi,
                                          id,
                                          tila,
                                          nimi,
                                          hakutapa_koodi_uri,
                                          kohdejoukko_koodi_uri,
                                          organisaatio_oid,
                                          metadata,
                                          esikatselu,
                                          kielivalinta,
                                          julkinen,
                                          sorakuvaus_id,
                                          muokkaaja,
                                          transaction_id,
                                          system_time)
    values (old.koulutustyyppi,
            old.id,
            old.tila,
            old.nimi,
            old.hakutapa_koodi_uri,
            old.kohdejoukko_koodi_uri,
            old.organisaatio_oid,
            old.metadata,
            old.esikatselu,
            old.kielivalinta,
            old.julkinen,
            old.sorakuvaus_id,
            old.muokkaaja,
            old.transaction_id,
            tstzrange(lower(old.system_time), now(), '[)'));
    return null;
end;
$$;
