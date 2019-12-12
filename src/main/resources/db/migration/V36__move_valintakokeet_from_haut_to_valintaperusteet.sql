-- Lisätään viimeiset valintakokeet historiatauluun ja poistetaan varsinainen taulu
delete from hakujen_valintakokeet;
drop table hakujen_valintakokeet;
drop function update_hakujen_valintakokeet_history;

create table valintaperusteiden_valintakokeet (
    id uuid primary key,
    valintaperuste_id uuid not null references valintaperusteet(id),
    tyyppi_koodi_uri varchar,
    tilaisuudet jsonb,
    muokkaaja varchar not null,
    transaction_id bigint not null default txid_current(),
    system_time tstzrange not null default tstzrange(now(), null, '[)')
);

COMMENT ON TABLE valintaperusteiden_valintakokeet IS 'Valintaperusteiden valintakokeiden tiedot';
COMMENT ON COLUMN valintaperusteiden_valintakokeet.id IS 'Valintakokeen yksilöivä tunniste. Järjestelmän generoima.';
COMMENT ON COLUMN valintaperusteiden_valintakokeet.valintaperuste_id IS 'Valintakokeen valintaperusteen yksilöivä tunniste.';
COMMENT ON COLUMN valintaperusteiden_valintakokeet.tyyppi_koodi_uri IS 'Valintakokeen tyyppi. Viittaa koodistoon: https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/valintakokeentyyppi/1';
COMMENT ON COLUMN valintaperusteiden_valintakokeet.tilaisuudet IS 'Valintakokeen järjestämistilaisuudet: osoite, aika ja lisätietoja ';
COMMENT ON COLUMN valintaperusteiden_valintakokeet.muokkaaja IS 'Valintakoetta viimeksi muokanneen virkailijan henkilö-oid';
COMMENT ON COLUMN valintaperusteiden_valintakokeet.transaction_id IS 'Transaktion, jossa viimeksi muokattu, tunniste';
COMMENT ON COLUMN valintaperusteiden_valintakokeet.system_time IS 'Valintakokeen viimeisin muokkausaika. Järjestelmän generoima';

create table valintaperusteiden_valintakokeet_history (like valintaperusteiden_valintakokeet);

create trigger set_temporal_columns_on_valintaperusteiden_valintakokeet_on_insert
    before insert on valintaperusteiden_valintakokeet
    for each row
execute procedure set_temporal_columns();

create trigger set_temporal_columns_on_valintaperusteiden_valintakokeet_on_update
    before update on valintaperusteiden_valintakokeet
    for each row
execute procedure set_temporal_columns();

create or replace function update_valintaperusteiden_valintakokeet_history() returns trigger as
$$
begin
    insert into valintaperusteiden_valintakokeet_history (
        id,
        valintaperuste_id,
        tyyppi_koodi_uri,
        tilaisuudet,
        muokkaaja,
        transaction_id,
        system_time
    ) values (old.id,
              old.valintaperuste_id,
              old.tyyppi_koodi_uri,
              old.tilaisuudet,
              old.muokkaaja,
              old.transaction_id,
              tstzrange(lower(old.system_time), now(), '[)'));
    return null;
end;
$$ language plpgsql;

create trigger valintaperusteiden_valintakokeet_history
    after update on valintaperusteiden_valintakokeet
    for each row
    when (old.transaction_id <> txid_current())
execute procedure update_valintaperusteiden_valintakokeet_history();

create trigger delete_valintaperusteiden_valintakokeet_history
    after delete on valintaperusteiden_valintakokeet
    for each row
execute procedure update_valintaperusteiden_valintakokeet_history();
