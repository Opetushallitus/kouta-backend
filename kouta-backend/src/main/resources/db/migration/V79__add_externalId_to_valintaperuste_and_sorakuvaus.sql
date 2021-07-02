-- Valintaperusteet
alter table valintaperusteet
    add column external_id varchar;
alter table valintaperusteet_history
    add column external_id varchar;

comment on column valintaperusteet.external_id is 'Ulkoinen tunniste jota voidaan käyttää Kouta lomakkeiden mäppäykseen oppilaitosten omien tietojärjestelmien kanssa';

create or replace function update_valintaperusteet_history() returns trigger
    language plpgsql
as
$$
begin
insert into valintaperusteet_history (
      koulutustyyppi,
      id,
      external_id,
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
        old.external_id,
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

-- Sorakuvaukset
alter table sorakuvaukset
    add column external_id varchar;
alter table sorakuvaukset_history
    add column external_id varchar;

comment on column sorakuvaukset.external_id is 'Ulkoinen tunniste jota voidaan käyttää Kouta lomakkeiden mäppäykseen oppilaitosten omien tietojärjestelmien kanssa';

create or replace function update_sorakuvaukset_history() returns trigger
    language plpgsql
as
$$
begin
    insert into sorakuvaukset_history (
        id,
        external_id,
        tila,
        nimi,
        koulutustyyppi,
        kielivalinta,
        metadata,
        organisaatio_oid,
        muokkaaja,
        transaction_id,
        system_time
    ) values (
         old.id,
         old.external_id,
         old.tila,
         old.nimi,
         old.koulutustyyppi,
         old.kielivalinta,
         old.metadata,
         old.organisaatio_oid,
         old.muokkaaja,
         old.transaction_id,
         tstzrange(lower(old.system_time), now(), '[)')
    );
    return null;
end;
$$;