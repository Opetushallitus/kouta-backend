-- Päivitetään haun last_modified ja muokkaaja kun sen hakuajat muuttuu, mutta vain jos last_modified muuttuisi.
-- Näin vältetään turhat muutokset haut-tauluun, kun useampi hakuaika muuttuu samalla
create or replace function set_haut_last_modified_from_related() returns trigger as
$$
begin
    if (tg_op = 'DELETE') then
        update haut
        set last_modified = now()::timestamptz
        where oid = old.haku_oid
          and last_modified <> now()::timestamptz;
        return null;
    else
        update haut
        set last_modified = now()::timestamptz, muokkaaja = new.muokkaaja
        where oid = new.haku_oid
          and last_modified <> now()::timestamptz;
        return null;
    end if;
end;
$$ language plpgsql;