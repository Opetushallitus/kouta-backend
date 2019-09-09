UPDATE valintaperusteet
SET metadata = metadata - 'koulutystyyppi' || jsonb_build_object('tyyppi', koulutustyyppi)
WHERE metadata is not null;

DO
$BODY$
    declare
        result record;
        t jsonb;
        new_array jsonb;
    BEGIN
        FOR result IN SELECT t.oid, t.metadata FROM toteutukset t
                                                        inner join koulutukset k on t.koulutus_oid = k.oid
                      where k.tyyppi = 'amm' and t.metadata is not null
            LOOP
                new_array = '[]'::JSONB;
                FOR t in select * from jsonb_array_elements(result.metadata->'osaamisalat')
                    loop
                        t = t - 'koodi' || jsonb_build_object('koodiUri', t->'koodi');
                        new_array = new_array || t;
                    end loop;
                IF jsonb_array_length(new_array) > 0 THEN
                    update toteutukset set metadata = jsonb_set(metadata, '{osaamisalat}', new_array, false);
                END IF;
            END LOOP;
    END;
$BODY$ language plpgsql;

alter table hakukohteiden_valintakokeet rename tyyppi to tyyppi_koodi_uri;
alter table hakukohteiden_valintakokeet_history rename tyyppi to tyyppi_koodi_uri;

create or replace function update_hakukohteiden_valintakokeet_history() returns trigger as
$$
begin
    insert into hakukohteiden_valintakokeet_history (
        id,
        hakukohde_oid,
        tyyppi_koodi_uri,
        tilaisuudet,
        muokkaaja,
        transaction_id,
        system_time
    ) values (old.id,
              old.hakukohde_oid,
              old.tyyppi_koodi_uri,
              old.tilaisuudet,
              old.muokkaaja,
              old.transaction_id,
              tstzrange(lower(old.system_time), now(), '[)'));
    return null;
end;
$$ language plpgsql;

alter table hakukohteiden_liitteet rename tyyppi to tyyppi_koodi_uri;
alter table hakukohteiden_liitteet_history rename tyyppi to tyyppi_koodi_uri;

create or replace function update_hakukohteiden_liitteet_history() returns trigger as
$$
begin
    insert into hakukohteiden_liitteet_history (
        id,
        hakukohde_oid,
        tyyppi_koodi_uri,
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
              old.tyyppi_koodi_uri,
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

alter table hakujen_valintakokeet rename tyyppi to tyyppi_koodi_uri;
alter table hakujen_valintakokeet_history rename tyyppi to tyyppi_koodi_uri;

create or replace function update_hakujen_valintakokeet_history() returns trigger as
$$
begin
    insert into hakujen_valintakokeet_history (
        id,
        haku_oid,
        tyyppi_koodi_uri,
        tilaisuudet,
        muokkaaja,
        transaction_id,
        system_time
    ) values (old.id,
              old.haku_oid,
              old.tyyppi_koodi_uri,
              old.tilaisuudet,
              old.muokkaaja,
              old.transaction_id,
              tstzrange(lower(old.system_time), now(), '[)'));
    return null;
end;
$$ language plpgsql;

