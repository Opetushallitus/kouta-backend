-- Korvataan vanhat triggerifunktiot päivitetyillä
drop function set_koulutukset_last_modified_from_related cascade;

-- Päivitetään koulutuksen last_modified ja muokkaaja kun sen tarjoajat päivittyy, mutta vain jos last_modified muuttuisi.
-- Näin vältetään turhat muutokset koulutukset-tauluun, kun useampi tarjoaja muuttuu samalla
create or replace function set_koulutukset_last_modified_from_related() returns trigger as
$$
declare
    tarjoaja_muokkaaja varchar;
begin
if (tg_op = 'DELETE') then
    tarjoaja_muokkaaja := old.muokkaaja;
else
    tarjoaja_muokkaaja := new.muokkaaja;
end if;
update koulutukset
set last_modified = now()::timestamptz, muokkaaja = tarjoaja_muokkaaja
where oid = old.koulutus_oid
  and last_modified <> now()::timestamptz;
return null;
end;
$$ language plpgsql;

create trigger set_last_modified_on_koulutusten_tarjoajat_change
    after insert or update or delete
                    on koulutusten_tarjoajat
                        for each row
                        execute procedure set_koulutukset_last_modified_from_related();