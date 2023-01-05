alter table koulutukset
    add column last_modified timestamptz;

alter table koulutukset_history
    add column last_modified timestamptz;

comment on column koulutukset.last_modified is 'Milloin koulutuksen tietoja on viimeksi muokattu (mukaan lukien tarjoajat)?';

create or replace function update_koulutukset_history() returns trigger
    language plpgsql
as
$$
begin
    insert into koulutukset_history (oid,
                                     external_id,
                                     johtaa_tutkintoon,
                                     tyyppi,
                                     koulutukset_koodi_uri,
                                     tila,
                                     nimi,
                                     metadata,
                                     muokkaaja,
                                     transaction_id,
                                     system_time,
                                     kielivalinta,
                                     organisaatio_oid,
                                     esikatselu,
                                     julkinen,
                                     teemakuva,
                                     eperuste_id,
                                     last_modified)
    values (old.oid,
            old.external_id,
            old.johtaa_tutkintoon,
            old.tyyppi,
            old.koulutukset_koodi_uri,
            old.tila,
            old.nimi,
            old.metadata,
            old.muokkaaja,
            old.transaction_id,
            tstzrange(lower(old.system_time), now(), '[)'),
            old.kielivalinta,
            old.organisaatio_oid,
            old.esikatselu,
            old.julkinen,
            old.teemakuva,
            old.eperuste_id,
            now());
    return null;
end;
$$;

-- Asetetaan vanhoille koulutuksille last_modified system_time-sarakkeiden perusteella
with koulutus_mod as (select k.oid                                 as oid,
                             greatest(max(lower(k.system_time)),
                                      max(lower(ta.system_time)),
                                      max(upper(tah.system_time))) as modified
                      from koulutukset k
                               left join koulutusten_tarjoajat ta on k.oid = ta.koulutus_oid
                               left join koulutusten_tarjoajat_history tah on k.oid = tah.koulutus_oid group by k.oid)
update koulutukset k
set last_modified = koulutus_mod.modified
from koulutus_mod
where k.oid = koulutus_mod.oid
  and k.last_modified is null;

-- Asetetaan koulutuksen last_modified kun koulutus itse muuttuu
create or replace function set_koulutus_last_modified() returns trigger as
$$
begin
    if tg_op = 'INSERT' or old.last_modified <> now() then
        new.last_modified := now()::timestamptz;
    end if;
    return new;
end;
$$ language plpgsql;

create trigger set_koulutus_last_modified_on_change
    before insert or update
    on koulutukset
    for each row
execute procedure set_koulutus_last_modified();

-- Asetetaan koulutuksen last_modified kun sen tarjoajat muuttuu
create or replace function set_koulutus_last_modified_from_tarjoajat() returns trigger as
$$
begin
    if tg_op = 'DELETE' then
        update koulutukset
        set last_modified = now()::timestamptz
        where oid = old.koulutus_oid
          and last_modified <> now()::timestamptz;
        return old;
    else
        update koulutukset
        set last_modified = now()::timestamptz
        where oid = new.koulutus_oid
          and last_modified <> now()::timestamptz;
        return new;
    end if;
end;
$$ language plpgsql;

create trigger set_koulutus_last_modified_on_tarjoajat_change
    before insert or update or delete
    on koulutusten_tarjoajat
    for each row
execute procedure set_koulutus_last_modified_from_tarjoajat();