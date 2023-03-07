-- Korvataan vanhat triggerifunktiot päivitetyillä
drop function set_toteutukset_last_modified_from_related cascade;

-- Päivitetään toteutuksen last_modified ja muokkaaja kun sen tarjoajat muuttuu, mutta vain jos last_modified muuttuisi.
-- Näin vältetään turhat muutokset toteutukset-tauluun, kun useampi tarjoaja muuttuu samalla
create or replace function set_toteutukset_last_modified_from_related() returns trigger as
$$
begin
    if (tg_op = 'DELETE') then
        update toteutukset
        set last_modified = now()::timestamptz, muokkaaja = old.muokkaaja
        where oid = old.toteutus_oid
          and last_modified <> now()::timestamptz;
        return null;
    else
        update toteutukset
        set last_modified = now()::timestamptz, muokkaaja = new.muokkaaja
        where oid = new.toteutus_oid
          and last_modified <> now()::timestamptz;
        return null;
    end if;
end;
$$ language plpgsql;

create trigger set_last_modified_on_toteutusten_tarjoajat_change
    after insert or update or delete
    on toteutusten_tarjoajat
    for each row
execute procedure set_toteutukset_last_modified_from_related();