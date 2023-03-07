alter table toteutukset
    add column last_modified timestamptz;

alter table toteutukset_history
    add column last_modified timestamptz;

comment on column toteutukset.last_modified is 'Milloin toteutuksen tietoja on viimeksi muokattu (mukaan lukien tarjoajat)?';

create or replace function update_toteutukset_history() returns trigger
    language plpgsql
as
$$
begin
    insert into toteutukset_history (oid,
                                     external_id,
                                     koulutus_oid,
                                     tila,
                                     nimi,
                                     metadata,
                                     muokkaaja,
                                     transaction_id,
                                     system_time,
                                     kielivalinta,
                                     esikatselu,
                                     organisaatio_oid,
                                     teemakuva,
                                     sorakuvaus_id,
                                     last_modified)
    values (old.oid,
            old.external_id,
            old.koulutus_oid,
            old.tila,
            old.nimi,
            old.metadata,
            old.muokkaaja,
            old.transaction_id,
            tstzrange(lower(old.system_time), now(), '[)'),
            old.kielivalinta,
            old.esikatselu,
            old.organisaatio_oid,
            old.teemakuva,
            old.sorakuvaus_id,
            old.last_modified);
    return null;
end;
$$;

-- Asetetaan vanhoille toteutuksille last_modified system_time-sarakkeiden perusteella
with toteutus_mod as (select t.oid                                 as oid,
                             greatest(max(lower(t.system_time)),
                                      max(lower(ta.system_time)),
                                      max(upper(tah.system_time))) as modified
                      from toteutukset t
                               left join toteutusten_tarjoajat ta on t.oid = ta.toteutus_oid
                               left join toteutusten_tarjoajat_history tah on t.oid = tah.toteutus_oid
                      group by t.oid)
update toteutukset t
set last_modified = toteutus_mod.modified
from toteutus_mod
where t.oid = toteutus_mod.oid
  and t.last_modified is null;

-- Asetetaan toteutuksen last_modified nykyhetkeen ennen kuin toteutus itse päivittyy
create trigger set_toteutukset_last_modified_on_change
    before insert or update
    on toteutukset
    for each row
execute procedure set_last_modified();

-- Päivitetään toteutuksen last_modified kun sen tarjoajat muuttuu, mutta vain jos last_modified muuttuisi.
-- Näin vältetään turhat muutokset toteutukset-tauluun, kun useampi tarjoaja muuttuu samalla
create or replace function set_toteutukset_last_modified_from_related() returns trigger as
$$
begin
    update toteutukset
    set last_modified = now()::timestamptz
    where oid = old.toteutus_oid
      and last_modified <> now()::timestamptz;
    return null;
end;
$$ language plpgsql;

create trigger set_last_modified_on_toteutusten_tarjoajat_change
    after insert or update or delete
    on toteutusten_tarjoajat
    for each row
execute procedure set_toteutukset_last_modified_from_related();