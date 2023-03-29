-- Päivitetään hakukohteen last_modified ja muokkaaja kun sen hakuajat, valintakokeet tai liitteet muuttuu, mutta vain jos last_modified muuttuisi.
-- Näin vältetään turhat muutokset hakukohteet-tauluun, kun useampi hakuaika, valintakoe tai liite muuttuu samalla
create or replace function set_hakukohteet_last_modified_from_related() returns trigger as
$$
begin
    if (tg_op = 'DELETE') then
        update hakukohteet
        set last_modified = now()::timestamptz
        where oid = old.hakukohde_oid
          and last_modified <> now()::timestamptz;
        return null;
    else
        update hakukohteet
        set last_modified = now()::timestamptz, muokkaaja = new.muokkaaja
        where oid = new.hakukohde_oid
          and last_modified <> now()::timestamptz;
        return null;
    end if;
end;
$$ language plpgsql;