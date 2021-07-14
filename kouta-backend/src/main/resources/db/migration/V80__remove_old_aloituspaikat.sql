alter table hakukohteet drop column if exists aloituspaikat;
alter table hakukohteet drop column if exists ensikertalaisen_aloituspaikat;
alter table hakukohteet_history drop column if exists aloituspaikat;
alter table hakukohteet_history drop column if exists ensikertalaisen_aloituspaikat;

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
                                 system_time)
values (old.oid,
        old.external_id,
        old.toteutus_oid,
        old.haku_oid,
        old.tila,
        old.nimi,
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
        tstzrange(lower(old.system_time), now(), '[)'));
return null;
end;
$$;