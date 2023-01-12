alter table haut
    add column last_modified timestamptz;

alter table haut_history
    add column last_modified timestamptz;

comment on column haut.last_modified is 'Milloin haun tietoja on viimeksi muokattu (mukaan lukien hakuajat)?';

create or replace function update_haut_history() returns trigger as
$$
begin
    insert into haut_history (oid,
                              external_id,
                              tila,
                              nimi,
                              hakutapa_koodi_uri,
                              hakukohteen_liittamisen_takaraja,
                              hakukohteen_muokkaamisen_takaraja,
                              ajastettu_julkaisu,
                              ajastettu_haun_ja_hakukohteiden_arkistointi,
                              ajastettu_haun_ja_hakukohteiden_arkistointi_ajettu,
                              kohdejoukko_koodi_uri,
                              kohdejoukon_tarkenne_koodi_uri,
                              hakulomaketyyppi,
                              hakulomake_ataru_id,
                              hakulomake_kuvaus,
                              hakulomake_linkki,
                              metadata,
                              muokkaaja,
                              organisaatio_oid,
                              kielivalinta,
                              transaction_id,
                              system_time,
                              last_modified)
    values (old.oid,
            old.external_id,
            old.tila,
            old.nimi,
            old.hakutapa_koodi_uri,
            old.hakukohteen_liittamisen_takaraja,
            old.hakukohteen_muokkaamisen_takaraja,
            old.ajastettu_julkaisu,
            old.ajastettu_haun_ja_hakukohteiden_arkistointi,
            old.ajastettu_haun_ja_hakukohteiden_arkistointi_ajettu,
            old.kohdejoukko_koodi_uri,
            old.kohdejoukon_tarkenne_koodi_uri,
            old.hakulomaketyyppi,
            old.hakulomake_ataru_id,
            old.hakulomake_kuvaus,
            old.hakulomake_linkki,
            old.metadata,
            old.muokkaaja,
            old.organisaatio_oid,
            old.kielivalinta,
            old.transaction_id,
            tstzrange(lower(old.system_time), now(), '[)'),
            old.last_modified);
    return null;
end;
$$ language plpgsql;

-- Asetetaan vanhoille hauille last_modified system_time-sarakkeiden perusteella
with haku_mod as (select ha.oid                               oid,
                                   greatest(
                                           max(lower(ha.system_time)),
                                           max(lower(hh.system_time)),
                                           max(upper(hhh.system_time))) modified
                            from haut ha
                                     left join hakujen_hakuajat hh on ha.oid = hh.haku_oid
                                     left join hakujen_hakuajat_history hhh on ha.oid = hhh.haku_oid
                            group by ha.oid)
update haut h
set last_modified = haku_mod.modified
from haku_mod
where h.oid = haku_mod.oid
  and h.last_modified is null;

-- Asetetaan haun last_modified nykyhetkeen ennen kuin haku itse päivittyy
create or replace function set_haku_last_modified() returns trigger as
$$
begin
    new.last_modified := now()::timestamptz;
    return new;
end;
$$ language plpgsql;

create trigger set_haku_last_modified_on_change
    before insert or update
    on haut
    for each row
execute procedure set_haku_last_modified();

-- Päivitetään haun last_modified kun sen hakuajat muuttuu, mutta vain jos last_modified muuttuisi.
-- Näin vältetään turhat muutokset haut-tauluun, kun useampi hakuaika muuttuu samalla
create or replace function set_haku_last_modified_from_related() returns trigger as
$$
begin
    update haut
    set last_modified = now()::timestamptz
    where oid = old.haku_oid
      and last_modified <> now()::timestamptz;
    return null;
end;
$$ language plpgsql;

create trigger set_haku_last_modified_on_hakuajat_change
    after insert or update or delete
    on hakujen_hakuajat
    for each row
execute procedure set_haku_last_modified_from_related();
