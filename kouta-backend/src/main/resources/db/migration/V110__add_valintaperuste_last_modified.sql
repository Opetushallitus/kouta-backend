alter table valintaperusteet
    add column last_modified timestamptz;

alter table valintaperusteet_history
    add column last_modified timestamptz;

comment on column valintaperusteet.last_modified is 'Milloin valintaperusteen tietoja on viimeksi muokattu (mukaan lukien valintakokeet)?';

create or replace function update_valintaperusteet_history() returns trigger
    language plpgsql
as
$$
begin
    insert into valintaperusteet_history (koulutustyyppi,
                                          id,
                                          external_id,
                                          tila,
                                          nimi,
                                          hakutapa_koodi_uri,
                                          kohdejoukko_koodi_uri,
                                          organisaatio_oid,
                                          metadata,
                                          esikatselu,
                                          kielivalinta,
                                          julkinen,
                                          muokkaaja,
                                          transaction_id,
                                          system_time,
                                          last_modified)
    values (old.koulutustyyppi,
            old.id,
            old.external_id,
            old.tila,
            old.nimi,
            old.hakutapa_koodi_uri,
            old.kohdejoukko_koodi_uri,
            old.organisaatio_oid,
            old.metadata,
            old.esikatselu,
            old.kielivalinta,
            old.julkinen,
            old.muokkaaja,
            old.transaction_id,
            tstzrange(lower(old.system_time), now(), '[)'),
            old.last_modified);
    return null;
end;
$$;


-- Asetetaan vanhoille valintaperusteille last_modified system_time-sarakkeiden perusteella
with valintaperuste_mod as (select vp.id                                id,
                                   greatest(
                                           max(lower(vp.system_time)),
                                           max(lower(vk.system_time)),
                                           max(upper(vph.system_time)),
                                           max(upper(vkh.system_time))) modified
                            from valintaperusteet vp
                                     left join valintaperusteet_history vph on vp.id = vph.id
                                     left join valintaperusteiden_valintakokeet vk on vp.id = vk.valintaperuste_id
                                     left join valintaperusteiden_valintakokeet_history vkh
                                               on vp.id = vkh.valintaperuste_id
                            group by vp.id)
update valintaperusteet vp
set last_modified = valintaperuste_mod.modified
from valintaperuste_mod
where vp.id = valintaperuste_mod.id
  and vp.last_modified is null;

-- Asetetaan valintaperusteen last_modified nykyhetkeen ennen kuin valintaperuste itse päivittyy
create trigger set_valintaperusteet_last_modified_on_change
    before insert or update
    on valintaperusteet
    for each row
execute procedure set_last_modified();

-- Päivitetään valintaperusteen last_modified kun sen valintakokeet muuttuu, mutta vain jos last_modified muuttuisi.
-- Näin vältetään turhat muutokset valintaperusteet-tauluun, kun useampi valintakoe muuttuu samalla
create or replace function set_valintaperusteet_last_modified_from_related() returns trigger as
$$
begin
    update valintaperusteet
    set last_modified = now()::timestamptz
    where id = old.valintaperuste_id
      and last_modified <> now()::timestamptz;
    return null;
end;
$$ language plpgsql;

create trigger set_valintaperusteet_last_modified_on_valintaperusteiden_valintakokeet_change
    after insert or update or delete
    on valintaperusteiden_valintakokeet
    for each row
execute procedure set_valintaperusteet_last_modified_from_related();
