-- Päivitetään valintaperusteen last_modified ja muokkaaja kun sen valintakokeet muuttuu, mutta vain jos last_modified muuttuisi.
-- Näin vältetään turhat muutokset valintaperusteet-tauluun, kun useampi valintakoe muuttuu samalla
create or replace function set_valintaperusteet_last_modified_from_related() returns trigger as
$$
begin
    if (tg_op = 'DELETE') then
        update valintaperusteet
        set last_modified = now()::timestamptz, muokkaaja = old.muokkaaja
        where id = old.valintaperuste_id
          and last_modified <> now()::timestamptz;
        return null;
    else
        update valintaperusteet
        set last_modified = now()::timestamptz, muokkaaja = new.muokkaaja
        where id = new.valintaperuste_id
          and last_modified <> now()::timestamptz;
        return null;
    end if;
end;
$$ language plpgsql;