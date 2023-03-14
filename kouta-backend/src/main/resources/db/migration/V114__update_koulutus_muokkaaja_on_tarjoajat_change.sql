-- Päivitetään koulutuksen last_modified ja muokkaaja kun sen tarjoajat päivittyy, mutta vain jos last_modified muuttuisi.
-- Näin vältetään turhat muutokset koulutukset-tauluun, kun useampi tarjoaja muuttuu samalla
create or replace function set_koulutukset_last_modified_from_related() returns trigger as
$$
begin
if (tg_op = 'DELETE') then
    update koulutukset
    set last_modified = now()::timestamptz
    where oid = old.koulutus_oid
      and last_modified <> now()::timestamptz;
else
    update koulutukset
    set last_modified = now()::timestamptz, muokkaaja = new.muokkaaja
    where oid = new.koulutus_oid
      and last_modified <> now()::timestamptz;
end if;
return null;
end;
$$ language plpgsql;