alter table valintaperusteiden_valintakokeet add column nimi jsonb;
alter table valintaperusteiden_valintakokeet_history add column nimi jsonb;

alter table valintaperusteiden_valintakokeet add column metadata jsonb;
alter table valintaperusteiden_valintakokeet_history add column metadata jsonb;

create or replace function update_valintaperusteiden_valintakokeet_history() returns trigger as
$$
begin
    insert into valintaperusteiden_valintakokeet_history (
        id,
        valintaperuste_id,
        tyyppi_koodi_uri,
        nimi,
        metadata,
        tilaisuudet,
        muokkaaja,
        transaction_id,
        system_time
    ) values (old.id,
              old.valintaperuste_id,
              old.tyyppi_koodi_uri,
              old.nimi,
              old.metadata,
              old.tilaisuudet,
              old.muokkaaja,
              old.transaction_id,
              tstzrange(lower(old.system_time), now(), '[)'));
    return null;
end;
$$ language plpgsql;

alter table hakukohteiden_valintakokeet add column nimi jsonb;
alter table hakukohteiden_valintakokeet_history add column nimi jsonb;

alter table hakukohteiden_valintakokeet add column metadata jsonb;
alter table hakukohteiden_valintakokeet_history add column metadata jsonb;

create or replace function update_hakukohteiden_valintakokeet_history() returns trigger as
$$
begin
    insert into hakukohteiden_valintakokeet_history (
        id,
        hakukohde_oid,
        tyyppi_koodi_uri,
        nimi,
        metadata,
        tilaisuudet,
        muokkaaja,
        transaction_id,
        system_time
    ) values (old.id,
              old.hakukohde_oid,
              old.tyyppi_koodi_uri,
              old.nimi,
              old.metadata,
              old.tilaisuudet,
              old.muokkaaja,
              old.transaction_id,
              tstzrange(lower(old.system_time), now(), '[)'));
    return null;
end;
$$ language plpgsql;