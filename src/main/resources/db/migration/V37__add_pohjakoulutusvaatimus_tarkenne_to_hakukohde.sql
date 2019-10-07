alter table hakukohteet
    add column pohjakoulutusvaatimus_tarkenne jsonb;

COMMENT ON COLUMN hakukohteet.pohjakoulutusvaatimus_tarkenne IS 'Tarkenne toisen asteen hakukohteen pohjakoulutusvaatimuksiin.';

alter table hakukohteet_history
    add column pohjakoulutusvaatimus_tarkenne jsonb;

create or replace function update_hakukohteet_history() returns trigger as
$$
begin
    insert into hakukohteet_history (
        oid,
        toteutus_oid,
        haku_oid,
        tila,
        nimi,
        alkamiskausi_koodi_uri,
        alkamisvuosi,
        kaytetaan_haun_alkamiskautta,
        hakulomaketyyppi,
        hakulomake_ataru_id,
        hakulomake_kuvaus,
        hakulomake_linkki,
        kaytetaan_haun_hakulomaketta,
        aloituspaikat,
        min_aloituspaikat,
        max_aloituspaikat,
        ensikertalaisen_aloituspaikat,
        min_ensikertalaisen_aloituspaikat,
        max_ensikertalaisen_aloituspaikat,
        pohjakoulutusvaatimus_koodi_urit,
        pohjakoulutusvaatimus_tarkenne,
        muu_pohjakoulutusvaatimus_kuvaus,
        toinen_aste_onko_kaksoistutkinto,
        kaytetaan_haun_aikataulua,
        valintaperuste_id,
        liitteet_onko_sama_toimitusaika,
        liitteet_onko_sama_toimitusosoite,
        liitteiden_toimitusaika,
        liitteiden_toimitustapa,
        liitteiden_toimitusosoite,
        metadata,
        muokkaaja,
        organisaatio_oid,
        kielivalinta,
        transaction_id,
        system_time
    ) values (
        old.oid,
        old.toteutus_oid,
        old.haku_oid,
        old.tila,
        old.nimi,
        old.alkamiskausi_koodi_uri,
        old.alkamisvuosi,
        old.kaytetaan_haun_alkamiskautta,
        old.hakulomaketyyppi,
        old.hakulomake_ataru_id,
        old.hakulomake_kuvaus,
        old.hakulomake_linkki,
        old.kaytetaan_haun_hakulomaketta,
        old.aloituspaikat,
        old.min_aloituspaikat,
        old.max_aloituspaikat,
        old.ensikertalaisen_aloituspaikat,
        old.min_ensikertalaisen_aloituspaikat,
        old.max_ensikertalaisen_aloituspaikat,
        old.pohjakoulutusvaatimus_koodi_urit,
        old.pohjakoulutusvaatimus_tarkenne,
        old.muu_pohjakoulutusvaatimus_kuvaus,
        old.toinen_aste_onko_kaksoistutkinto,
        old.kaytetaan_haun_aikataulua,
        old.valintaperuste_id,
        old.liitteet_onko_sama_toimitusaika,
        old.liitteet_onko_sama_toimitusosoite,
        old.liitteiden_toimitusaika,
        old.liitteiden_toimitustapa,
        old.liitteiden_toimitusosoite,
        old.metadata,
        old.muokkaaja,
        old.organisaatio_oid,
        old.kielivalinta,
        old.transaction_id,
        tstzrange(lower(old.system_time), now(), '[)'));
    return null;
end;
$$ language plpgsql;
