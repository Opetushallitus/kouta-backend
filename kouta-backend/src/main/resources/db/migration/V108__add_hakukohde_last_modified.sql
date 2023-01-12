alter table hakukohteet
    add column last_modified timestamptz;

alter table hakukohteet_history
    add column last_modified timestamptz;

comment on column hakukohteet.last_modified is 'Milloin hakukohteen tietoja on viimeksi muokattu (mukaan lukien hakuajat, valintakokeet ja liitteet)?';

create or replace function update_hakukohteet_history() returns trigger
    language plpgsql
as
$$
begin
    insert into hakukohteet_history (oid,
                                     external_id,
                                     toteutus_oid,
                                     haku_oid,
                                     tila,
                                     nimi,
                                     hakukohde_koodi_uri,
                                     hakulomaketyyppi,
                                     hakulomake_ataru_id,
                                     hakulomake_kuvaus,
                                     hakulomake_linkki,
                                     kaytetaan_haun_hakulomaketta,
                                     jarjestyspaikka_oid,
                                     pohjakoulutusvaatimus_koodi_urit,
                                     muu_pohjakoulutusvaatimus_kuvaus,
                                     pohjakoulutusvaatimus_tarkenne,
                                     toinen_aste_onko_kaksoistutkinto,
                                     kaytetaan_haun_aikataulua,
                                     valintaperuste_id,
                                     liitteet_onko_sama_toimitusaika,
                                     liitteet_onko_sama_toimitusosoite,
                                     liitteiden_toimitusaika,
                                     liitteiden_toimitustapa,
                                     liitteiden_toimitusosoite,
                                     esikatselu,
                                     metadata,
                                     muokkaaja,
                                     organisaatio_oid,
                                     kielivalinta,
                                     transaction_id,
                                     system_time,
                                     last_modified)
    values (old.oid,
            old.external_id,
            old.toteutus_oid,
            old.haku_oid,
            old.tila,
            old.nimi,
            old.hakukohde_koodi_uri,
            old.hakulomaketyyppi,
            old.hakulomake_ataru_id,
            old.hakulomake_kuvaus,
            old.hakulomake_linkki,
            old.kaytetaan_haun_hakulomaketta,
            old.jarjestyspaikka_oid,
            old.pohjakoulutusvaatimus_koodi_urit,
            old.muu_pohjakoulutusvaatimus_kuvaus,
            old.pohjakoulutusvaatimus_tarkenne,
            old.toinen_aste_onko_kaksoistutkinto,
            old.kaytetaan_haun_aikataulua,
            old.valintaperuste_id,
            old.liitteet_onko_sama_toimitusaika,
            old.liitteet_onko_sama_toimitusosoite,
            old.liitteiden_toimitusaika,
            old.liitteiden_toimitustapa,
            old.liitteiden_toimitusosoite,
            old.esikatselu,
            old.metadata,
            old.muokkaaja,
            old.organisaatio_oid,
            old.kielivalinta,
            old.transaction_id,
            tstzrange(lower(old.system_time), now(), '[)'),
            old.last_modified);
    return null;
end;
$$;

-- Asetetaan vanhoille hakukohteille last_modified system_time-sarakkeiden perusteella
with hakukohde_mod as (select ha.oid                               as oid,
                              greatest(
                                      max(lower(ha.system_time)),
                                      max(lower(hh.system_time)),
                                      max(lower(hv.system_time)),
                                      max(lower(hl.system_time)),
                                      max(upper(hhh.system_time)),
                                      max(upper(hvh.system_time)),
                                      max(upper(hlh.system_time))) as modified
                       from hakukohteet ha
                                left join hakukohteiden_hakuajat hh on ha.oid = hh.hakukohde_oid
                                left join hakukohteiden_hakuajat_history hhh on ha.oid = hhh.hakukohde_oid
                                left join hakukohteiden_valintakokeet hv on ha.oid = hv.hakukohde_oid
                                left join hakukohteiden_valintakokeet_history hvh on ha.oid = hvh.hakukohde_oid
                                left join hakukohteiden_liitteet hl on ha.oid = hl.hakukohde_oid
                                left join hakukohteiden_liitteet_history hlh on ha.oid = hlh.hakukohde_oid
                       group by ha.oid)
update hakukohteet ha
set last_modified = hakukohde_mod.modified
from hakukohde_mod
where ha.oid = hakukohde_mod.oid
  and ha.last_modified is null;

-- Asetetaan hakukohteen last_modified nykyhetkeen ennen kuin hakukohde itse päivittyy
create or replace function set_hakukohde_last_modified() returns trigger as
$$
begin
    new.last_modified := now()::timestamptz;
    return new;
end;
$$ language plpgsql;

create trigger set_hakukohde_last_modified_on_change
    before insert or update
    on hakukohteet
    for each row
execute procedure set_hakukohde_last_modified();

-- Päivitetään hakukohteen last_modified kun sen hakuajat, valintakokeet tai liitteet muuttuu, mutta vain jos last_modified muuttuisi.
-- Näin vältetään turhat muutokset hakukohteet-tauluun, kun useampi hakuaika, valintakoe tai liite muuttuu samalla
create or replace function set_hakukohde_last_modified_from_related() returns trigger as
$$
begin
    update hakukohteet
    set last_modified = now()::timestamptz
    where oid = old.hakukohde_oid
      and last_modified <> now()::timestamptz;
    return null;
end;
$$ language plpgsql;

create trigger set_hakukohde_last_modified_on_hakuajat_change
    after insert or update or delete
    on hakukohteiden_hakuajat
    for each row
execute procedure set_hakukohde_last_modified_from_related();

create trigger set_hakukohde_last_modified_on_valintakokeet_change
    after insert or update or delete
    on hakukohteiden_valintakokeet
    for each row
execute procedure set_hakukohde_last_modified_from_related();

create trigger set_hakukohde_last_modified_on_hakuajat_change
    after insert or update or delete
    on hakukohteiden_liitteet
    for each row
execute procedure set_hakukohde_last_modified_from_related();